import { create } from 'zustand';
import type { ConversationResponse, MessageResponse, UserPresenceResponse } from '../types/dto/chat.dto';
import type { ConversationUpdateMessage } from '../types/dto/message-summary.dto';

interface ChatState {
  conversations: ConversationResponse[];
  activeConversation: string | null;
  onlineUsers: Map<number, boolean>;
  messages: Map<string, MessageResponse[]>;
  
  // Actions
  setConversations: (conversations: ConversationResponse[]) => void;
  setActiveConversation: (conversationId: string | null) => void;
  updateUserPresence: (userId: number, isOnline: boolean) => void;
  addMessage: (conversationId: string, message: MessageResponse) => void;
  setMessages: (conversationId: string, messages: MessageResponse[]) => void;
  updateConversationLastMessage: (conversationId: string, message: MessageResponse) => void;
  updateConversation: (update: ConversationUpdateMessage) => void;
  incrementUnreadCount: (conversationId: string) => void;
  resetUnreadCount: (conversationId: string) => void;
  clearChatData: () => void;
}

export const useChatStore = create<ChatState>((set, get) => ({
  conversations: [],
  activeConversation: null,
  onlineUsers: new Map(),
  messages: new Map(),

  setConversations: (conversations: ConversationResponse[]) => {
    set({ conversations });
  },

  setActiveConversation: (conversationId: string | null) => {
    set({ activeConversation: conversationId });
  },

  updateUserPresence: (userId: number, isOnline: boolean) => {
    set((state) => {
      const newOnlineUsers = new Map(state.onlineUsers);
      newOnlineUsers.set(userId, isOnline);
      return { onlineUsers: newOnlineUsers };
    });
  },

  addMessage: (conversationId: string, message: MessageResponse) => {
    // Just update the conversation's last message
    get().updateConversationLastMessage(conversationId, message);
  },

  setMessages: (conversationId: string, messages: MessageResponse[]) => {
    set((state) => {
      const newMessages = new Map(state.messages);
      newMessages.set(conversationId, messages);
      return { messages: newMessages };
    });
  },

  updateConversationLastMessage: (conversationId: string, message: MessageResponse) => {
    set((state) => {
      const conversations = state.conversations.map(conv => {
        if (conv.id === conversationId) {
          return {
            ...conv,
            lastMessage: message,
            updatedAt: message.sentAt,
          };
        }
        return conv;
      });
      
      // Sort conversations by updatedAt (most recent first)
      conversations.sort((a, b) => 
        new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
      );
      
      return { conversations };
    });
  },

  updateConversation: (update: ConversationUpdateMessage) => {
    set((state) => {
      const existingIndex = state.conversations.findIndex(
        conv => conv.id === update.conversationId
      );
      
      if (existingIndex >= 0) {
        // Update existing conversation
        const updatedConversations = [...state.conversations];
        const existingConv = updatedConversations[existingIndex];
        
        updatedConversations[existingIndex] = {
          ...existingConv,
          name: update.conversationName || existingConv.name,
          lastMessage: {
            id: update.lastMessage.id,
            conversationId: update.lastMessage.conversationId,
            senderId: update.lastMessage.senderId,
            senderName: update.lastMessage.senderName,
            content: update.lastMessage.previewText,
            attachments: [],
            type: update.lastMessage.type,
            sentAt: update.lastMessage.sentAt,
            statusList: []
          },
          unreadCount: update.unreadCount,
          updatedAt: update.updatedAt,
          participants: update.participants.map(p => ({
            userId: p.userId,
            username: p.username,
            email: p.displayName || p.username,
            joinedAt: existingConv.participants.find(ep => ep.userId === p.userId)?.joinedAt || new Date().toISOString()
          }))
        };
        
        // Sort by updatedAt (most recent first)
        updatedConversations.sort((a, b) => 
          new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
        );
        
        return { conversations: updatedConversations };
      } else {
        // Add new conversation
        const newConversation: ConversationResponse = {
          id: update.conversationId,
          type: update.conversationType,
          name: update.conversationName,
          participants: update.participants.map(p => ({
            userId: p.userId,
            username: p.username,
            email: p.displayName || p.username,
            joinedAt: new Date().toISOString()
          })),
          lastMessage: {
            id: update.lastMessage.id,
            conversationId: update.lastMessage.conversationId,
            senderId: update.lastMessage.senderId,
            senderName: update.lastMessage.senderName,
            content: update.lastMessage.previewText,
            attachments: [],
            type: update.lastMessage.type,
            sentAt: update.lastMessage.sentAt,
            statusList: []
          },
          unreadCount: update.unreadCount,
          createdAt: new Date().toISOString(),
          updatedAt: update.updatedAt
        };
        
        // Insert new conversation and sort by updatedAt (most recent first)
        const allConversations = [newConversation, ...state.conversations];
        allConversations.sort((a, b) => 
          new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime()
        );
        
        return { conversations: allConversations };
      }
    });
  },

  incrementUnreadCount: (conversationId: string) => {
    set((state) => {
      const conversations = state.conversations.map(conv => {
        if (conv.id === conversationId && conv.id !== state.activeConversation) {
          return {
            ...conv,
            unreadCount: conv.unreadCount + 1,
          };
        }
        return conv;
      });
      
      return { conversations };
    });
  },

  resetUnreadCount: (conversationId: string) => {
    set((state) => {
      const conversations = state.conversations.map(conv => {
        if (conv.id === conversationId) {
          return {
            ...conv,
            unreadCount: 0,
          };
        }
        return conv;
      });
      
      return { conversations };
    });
  },

  clearChatData: () => {
    set({
      conversations: [],
      activeConversation: null,
      onlineUsers: new Map(),
      messages: new Map(),
    });
  },
}));
