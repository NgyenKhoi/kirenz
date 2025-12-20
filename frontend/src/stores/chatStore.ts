import { create } from 'zustand';
import type { ConversationResponse, MessageResponse, UserPresenceResponse } from '../types/dto/chat.dto';

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
