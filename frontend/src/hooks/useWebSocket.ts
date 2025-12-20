import { useEffect, useRef, useState } from 'react';
import { websocketService } from '../services';
import { useAuthStore } from '../stores/authStore';

interface UseWebSocketOptions {
  autoConnect?: boolean;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: any) => void;
}

export const useWebSocket = (options: UseWebSocketOptions = {}) => {
  const { autoConnect = false, onConnect, onDisconnect, onError } = options;
  const [isConnected, setIsConnected] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const accessToken = useAuthStore((state) => state.accessToken);
  const hasInitialized = useRef(false);

  useEffect(() => {
    if (autoConnect && accessToken && !hasInitialized.current) {
      hasInitialized.current = true;
      connect();
    }

    return () => {
      if (hasInitialized.current) {
        disconnect();
      }
    };
  }, [autoConnect, accessToken]);

  const connect = async () => {
    if (!accessToken) {
      console.error('[useWebSocket] No access token available');
      return;
    }

    if (isConnecting || isConnected) {
      return;
    }

    setIsConnecting(true);

    try {
      await websocketService.connect(accessToken);
      setIsConnected(true);
      setIsConnecting(false);
      
      if (onConnect) {
        onConnect();
      }

      websocketService.onDisconnect(() => {
        setIsConnected(false);
        if (onDisconnect) {
          onDisconnect();
        }
      });

      if (onError) {
        websocketService.onError(onError);
      }
    } catch (error) {
      console.error('[useWebSocket] Connection failed:', error);
      setIsConnecting(false);
      setIsConnected(false);
      
      if (onError) {
        onError(error);
      }
    }
  };

  const disconnect = () => {
    websocketService.disconnect();
    setIsConnected(false);
    setIsConnecting(false);
    hasInitialized.current = false;
  };

  const subscribe = (destination: string, callback: (message: any) => void): string => {
    if (!isConnected) {
      throw new Error('WebSocket is not connected');
    }
    return websocketService.subscribe(destination, callback);
  };

  const unsubscribe = (subscriptionId: string) => {
    websocketService.unsubscribe(subscriptionId);
  };

  const sendMessage = (destination: string, body: any) => {
    if (!isConnected) {
      throw new Error('WebSocket is not connected');
    }
    websocketService.sendMessage(destination, body);
  };

  return {
    isConnected,
    isConnecting,
    connect,
    disconnect,
    subscribe,
    unsubscribe,
    sendMessage
  };
};
