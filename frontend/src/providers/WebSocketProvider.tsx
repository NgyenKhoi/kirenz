import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { useChatStore } from '@/stores/chatStore';
import websocketService from '@/services/websocket.service';
import { chatKeys } from '@/hooks/queries/useChatQueries';
import { toast } from 'sonner';
import type { ConversationResponse, MessageResponse, UserPresenceResponse } from '@/types/dto/chat.dto';
import type { ConversationUpdateMessage } from '@/types/dto/message-summary.dto';

interface WebSocketProviderProps {
  children: React.ReactNode;
}

export const WebSocketProvider = ({ children }: WebSocketProviderProps) => {
  const queryClient = useQueryClient();
  const { userId, accessToken, isAuthenticated } = useAuthStore();
  const { updateConversation, addMessage, incrementUnreadCount } = useChatStore();
  const isInitialized = useRef(false);
  const subscriptionIds = useRef<string[]>([]);
  const previousToken = useRef<string | null>(null);

  // Handle token refresh - reconnect WebSocket with new token
  useEffect(() => {
    if (!isAuthenticated || !accessToken || !userId) {
      return;
    }

    if (previousToken.current && previousToken.current !== accessToken && isInitialized.current) {
      console.log('[WebSocketProvider] Token refreshed, reconnecting WebSocket...');
      
      const reconnectWebSocket = async () => {
        try {
          subscriptionIds.current.forEach(id => {
            websocketService.unsubscribe(id);
          });
          subscriptionIds.current = [];
          
          await websocketService.reconnect(accessToken, userId);
          
          await setupSubscriptions();
          
          console.log('[WebSocketProvider] Successfully reconnected after token refresh');
        } catch (error) {
          console.error('[WebSocketProvider] Failed to reconnect after token refresh:', error);
          isInitialized.current = false;
        }
      };
      
      reconnectWebSocket();
    }
    
    previousToken.current = accessToken;
  }, [accessToken, isAuthenticated, userId, queryClient]);

  const setupSubscriptions = async () => {
    // Subscribe to user-specific presence updates using RabbitMQ STOMP format
    const presenceSub = websocketService.subscribe(`/queue/presence.${userId}`, (presence: UserPresenceResponse) => {
      console.log('[WebSocketProvider] Presence update:', presence);
      if (presence.userId && presence.status) {
        const isOnline = presence.status === 'ONLINE';
        useChatStore.getState().updateUserPresence(presence.userId, isOnline);
      }
    });
    subscriptionIds.current.push(presenceSub);

    // Subscribe to user-specific conversation updates using RabbitMQ STOMP format
    const conversationSub = websocketService.subscribe(`/queue/conversations.${userId}`, (conversation: ConversationResponse) => {
      console.log('[WebSocketProvider] New conversation received:', conversation);
      queryClient.invalidateQueries({ queryKey: chatKeys.conversations() });
      toast.success('New conversation started');
    });
    subscriptionIds.current.push(conversationSub);

    // User queue subscription is now handled automatically by WebSocketService
    // when connecting with userId for conversation list updates
    websocketService.subscribeToUserQueue(userId!, (update: ConversationUpdateMessage) => {
      console.log('[WebSocketProvider] Conversation update received for conversation list:', update);
      
      const currentActiveConversation = useChatStore.getState().activeConversation;
      
      // Update conversation in store (conversation list)
      updateConversation(update);
      
      // If this is not the active conversation and not from current user, show notification
      if (update.conversationId !== currentActiveConversation && update.lastMessage.senderId !== userId) {
        toast.info(`New message from ${update.lastMessage.senderName}`);
      }
      
      // Invalidate queries to refresh data
      queryClient.invalidateQueries({ 
        queryKey: chatKeys.messages(update.conversationId, 0) 
      });
      
      queryClient.invalidateQueries({ 
        queryKey: chatKeys.conversations() 
      });
    });
  };

  useEffect(() => {
    // Only connect if authenticated and not already initialized
    if (!isAuthenticated || !userId || !accessToken || isInitialized.current) {
      return;
    }

    const initializeWebSocket = async () => {
      try {
        console.log('[WebSocketProvider] Initializing WebSocket connection...');
        
        // Load conversations first
        const conversationsData = await queryClient.fetchQuery({
          queryKey: chatKeys.conversations(),
          queryFn: async () => {
            const { chatApi } = await import('@/api/chatApi');
            const response = await chatApi.getConversations();
            return response.result || [];
          },
        });
        
        if (conversationsData) {
          useChatStore.getState().setConversations(conversationsData);
        }
        
        // Connect WebSocket with userId for automatic user queue subscription
        await websocketService.connect(accessToken, userId);
        isInitialized.current = true;
        previousToken.current = accessToken;

        // Setup additional subscriptions
        await setupSubscriptions();

        console.log('[WebSocketProvider] WebSocket initialized successfully');
      } catch (error) {
        console.error('[WebSocketProvider] Failed to initialize WebSocket:', error);
        isInitialized.current = false;
      }
    };

    initializeWebSocket();

    return () => {
      console.log('[WebSocketProvider] Cleaning up WebSocket connection...');
      subscriptionIds.current.forEach(id => {
        websocketService.unsubscribe(id);
      });
      subscriptionIds.current = [];
      websocketService.disconnect();
      isInitialized.current = false;
    };
  }, [isAuthenticated, userId, accessToken, queryClient, updateConversation]);

  return <>{children}</>;
};
