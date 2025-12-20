import { useEffect, useRef } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { useChatStore } from '@/stores/chatStore';
import websocketService from '@/services/websocket.service';
import { chatKeys } from '@/hooks/queries/useChatQueries';
import { toast } from 'sonner';
import type { ConversationResponse, MessageResponse, UserPresenceResponse } from '@/types/dto/chat.dto';

interface WebSocketProviderProps {
  children: React.ReactNode;
}

export const WebSocketProvider = ({ children }: WebSocketProviderProps) => {
  const queryClient = useQueryClient();
  const { userId, accessToken, isAuthenticated } = useAuthStore();
  const isInitialized = useRef(false);
  const subscriptionIds = useRef<string[]>([]);
  const previousToken = useRef<string | null>(null);

  // Handle token refresh - reconnect WebSocket with new token
  useEffect(() => {
    if (!isAuthenticated || !accessToken || !userId) {
      return;
    }

    // Check if token changed (token refresh happened)
    if (previousToken.current && previousToken.current !== accessToken && isInitialized.current) {
      console.log('[WebSocketProvider] Token refreshed, reconnecting WebSocket...');
      
      const reconnectWebSocket = async () => {
        try {
          // Unsubscribe from all current subscriptions
          subscriptionIds.current.forEach(id => {
            websocketService.unsubscribe(id);
          });
          subscriptionIds.current = [];
          
          // Reconnect with new token
          await websocketService.reconnect(accessToken);
          
          // Re-establish subscriptions
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
    // Subscribe to presence updates
    const presenceSub = websocketService.subscribe('/user/queue/presence', (presence: UserPresenceResponse) => {
      console.log('[WebSocketProvider] Presence update:', presence);
      if (presence.userId && presence.status) {
        const isOnline = presence.status === 'ONLINE';
        useChatStore.getState().updateUserPresence(presence.userId, isOnline);
      }
    });
    subscriptionIds.current.push(presenceSub);

    // Subscribe to new conversations
    const conversationSub = websocketService.subscribe('/user/queue/conversations', (conversation: ConversationResponse) => {
      console.log('[WebSocketProvider] New conversation received:', conversation);
      queryClient.invalidateQueries({ queryKey: chatKeys.conversations() });
      toast.success('New conversation started');
    });
    subscriptionIds.current.push(conversationSub);

    // Subscribe to new messages
    const messageSub = websocketService.subscribe('/user/queue/messages', (message: MessageResponse) => {
      console.log('[WebSocketProvider] New message received:', message);
      
      const currentActiveConversation = useChatStore.getState().activeConversation;
      useChatStore.getState().addMessage(message.conversationId, message);
      
      if (message.conversationId !== currentActiveConversation && message.senderId !== userId) {
        useChatStore.getState().incrementUnreadCount(message.conversationId);
      }
      
      queryClient.invalidateQueries({ 
        queryKey: chatKeys.messages(message.conversationId, 0) 
      });
      
      queryClient.invalidateQueries({ 
        queryKey: chatKeys.conversations() 
      });
      
      if (message.conversationId !== currentActiveConversation && message.senderId !== userId) {
        toast.info(`New message from ${message.senderName}`);
      }
    });
    subscriptionIds.current.push(messageSub);
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
        
        // Then connect WebSocket
        await websocketService.connect(accessToken);
        isInitialized.current = true;
        previousToken.current = accessToken;

        // Setup subscriptions
        await setupSubscriptions();

        console.log('[WebSocketProvider] WebSocket initialized successfully');
      } catch (error) {
        console.error('[WebSocketProvider] Failed to initialize WebSocket:', error);
        isInitialized.current = false;
      }
    };

    initializeWebSocket();

    // Cleanup on unmount or when auth changes
    return () => {
      console.log('[WebSocketProvider] Cleaning up WebSocket connection...');
      subscriptionIds.current.forEach(id => {
        websocketService.unsubscribe(id);
      });
      subscriptionIds.current = [];
      websocketService.disconnect();
      isInitialized.current = false;
    };
  }, [isAuthenticated, userId, accessToken, queryClient]);

  return <>{children}</>;
};
