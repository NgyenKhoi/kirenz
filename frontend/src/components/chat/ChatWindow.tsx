import { useEffect, useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { Card } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ArrowLeft, MoreVertical, Users } from "lucide-react";
import { cn } from "@/lib/utils";
import MessageList from "./MessageList";
import MessageInput from "./MessageInput";
import TypingIndicator from "./TypingIndicator";
import PresenceIndicator from "./PresenceIndicator";
import type { ConversationResponse, MessageResponse, TypingIndicator as TypingIndicatorType } from "@/types/dto/chat.dto";
import { useChatStore } from "@/stores/chatStore";
import { useMarkAsRead, useMessages, chatKeys } from "@/hooks/queries/useChatQueries";
import websocketService from "@/services/websocket.service";
import { toast } from "sonner";

interface ChatWindowProps {
  conversation: ConversationResponse;
  currentUserId: number;
  onBack?: () => void;
  className?: string;
}

const ChatWindow = ({ 
  conversation, 
  currentUserId,
  onBack,
  className 
}: ChatWindowProps) => {
  const queryClient = useQueryClient();
  const { onlineUsers, addMessage, resetUnreadCount } = useChatStore();
  const markAsReadMutation = useMarkAsRead();
  
  const [currentPage] = useState(0);
  const { data: messagesData, isLoading: isLoadingMessages } = useMessages(conversation.id, currentPage, 50);
  const conversationMessages = messagesData || [];
  
  const [typingUsers, setTypingUsers] = useState<Map<number, string>>(new Map());
  const [typingTimeout, setTypingTimeout] = useState<NodeJS.Timeout | null>(null);

  const getConversationName = (): string => {
    if (conversation.type === 'GROUP') {
      return conversation.name || 'Group Chat';
    }
    
    const otherParticipant = conversation.participants.find(
      p => p.userId !== currentUserId
    );
    return otherParticipant?.username || 'Unknown User';
  };

  const getOtherParticipantId = (): number | null => {
    if (conversation.type === 'DIRECT') {
      const otherParticipant = conversation.participants.find(
        p => p.userId !== currentUserId
      );
      return otherParticipant?.userId || null;
    }
    return null;
  };

  const isUserOnline = (userId: number | null): boolean => {
    if (userId === null) return false;
    return onlineUsers.get(userId) || false;
  };

  useEffect(() => {
    // Mark conversation as read and reset unread count only once when conversation changes
    if (conversation.unreadCount > 0) {
      markAsReadMutation.mutate(conversation.id);
      resetUnreadCount(conversation.id);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [conversation.id]);

  useEffect(() => {
    // Setup WebSocket subscriptions
    if (!websocketService.isConnected()) {
      return;
    }

    // Subscribe to conversation topic for active chat window (receives full message details)
    const messageSubscriptionId = websocketService.subscribeToConversation(
      conversation.id,
      (message: MessageResponse) => {
        console.log('📨 Received message via WebSocket for active chat window:', message);
        addMessage(conversation.id, message);
        // Invalidate messages query to refetch
        queryClient.invalidateQueries({ queryKey: chatKeys.messages(conversation.id, currentPage) });
      }
    );

    const typingSubscriptionId = websocketService.subscribe(
      `/topic/typing.${conversation.id}`,
      (indicator: TypingIndicatorType) => {
        if (indicator.userId !== currentUserId) {
          setTypingUsers(prev => {
            const newMap = new Map(prev);
            if (indicator.isTyping) {
              newMap.set(indicator.userId, indicator.username);
            } else {
              newMap.delete(indicator.userId);
            }
            return newMap;
          });

          setTimeout(() => {
            setTypingUsers(prev => {
              const newMap = new Map(prev);
              newMap.delete(indicator.userId);
              return newMap;
            });
          }, 3000);
        }
      }
    );

    return () => {
      // Unsubscribe from conversation when component unmounts or conversation changes
      if (messageSubscriptionId) {
        websocketService.unsubscribeFromConversation(conversation.id);
      }
      if (typingSubscriptionId) {
        websocketService.unsubscribe(typingSubscriptionId);
      }
    };
  }, [conversation.id, currentUserId, currentPage, addMessage, queryClient]);

  const handleSendMessage = async (content: string, attachments?: any[]) => {
    try {
      console.log('📤 Sending message with attachments:', attachments?.length || 0);
      
      // Send message with already-uploaded media URLs via WebSocket
      websocketService.sendMessage('/app/chat.send', {
        conversationId: conversation.id,
        content,
        attachments
      });
      
      console.log('✅ Message sent via WebSocket');
      
      // Invalidate conversations to update last message
      queryClient.invalidateQueries({ queryKey: chatKeys.conversations() });
    } catch (error) {
      console.error('❌ Failed to send message:', error);
      toast.error('Failed to send message');
    }
  };

  const handleTyping = () => {
    if (typingTimeout) {
      clearTimeout(typingTimeout);
    }

    // Get current user's display name
    const currentUser = conversation.participants.find(p => p.userId === currentUserId);
    const username = currentUser?.username || 'User';

    try {
      websocketService.sendMessage('/app/chat.typing', {
        conversationId: conversation.id,
        userId: currentUserId,
        username: username,
        isTyping: true
      });

      const timeout = setTimeout(() => {
        websocketService.sendMessage('/app/chat.typing', {
          conversationId: conversation.id,
          userId: currentUserId,
          username: username,
          isTyping: false
        });
      }, 2000);

      setTypingTimeout(timeout);
    } catch (error) {
      console.error('Failed to send typing indicator:', error);
    }
  };

  const conversationName = getConversationName();
  const otherParticipantId = getOtherParticipantId();
  const isOnline = isUserOnline(otherParticipantId);
  const typingUsersList = Array.from(typingUsers.values());

  return (
    <Card className={cn("flex flex-col", className)} style={{ height: 'calc(100vh - 200px)' }}>
      <div className="flex items-center justify-between border-b border-border p-4 shrink-0">
        <div className="flex items-center gap-3">
          {onBack && (
            <Button
              variant="ghost"
              size="icon"
              onClick={onBack}
              className="lg:hidden"
            >
              <ArrowLeft className="h-5 w-5" />
            </Button>
          )}
          
          <div className="flex items-center gap-3">
            <div>
              <h2 className="font-semibold text-lg">{conversationName}</h2>
              {conversation.type === 'DIRECT' ? (
                <div className="flex items-center gap-2">
                  <PresenceIndicator isOnline={isOnline} size="sm" />
                  <span className="text-xs text-muted-foreground">
                    {isOnline ? 'Online' : 'Offline'}
                  </span>
                </div>
              ) : (
                <div className="flex items-center gap-1 text-xs text-muted-foreground">
                  <Users className="h-3 w-3" />
                  <span>{conversation.participants.length} members</span>
                </div>
              )}
            </div>
          </div>
        </div>

        <Button variant="ghost" size="icon">
          <MoreVertical className="h-5 w-5" />
        </Button>
      </div>

      <MessageList
        messages={conversationMessages}
        currentUserId={currentUserId}
        isLoading={isLoadingMessages}
      />

      {typingUsersList.length > 0 && (
        <TypingIndicator username={typingUsersList[0]} />
      )}

      <div className="shrink-0">
        <MessageInput
          onSendMessage={handleSendMessage}
          onTyping={handleTyping}
          disabled={!websocketService.isConnected()}
          placeholder={`Message ${conversationName}...`}
        />
      </div>
    </Card>
  );
};

export default ChatWindow;
