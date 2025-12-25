import SockJS from 'sockjs-client';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

type MessageCallback = (message: any) => void;
type ConnectionCallback = () => void;
type ErrorCallback = (error: any) => void;

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private conversationSubscriptions: Map<string, StompSubscription> = new Map();
  private userQueueSubscription: StompSubscription | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;
  private isConnecting = false;
  private connectionCallbacks: ConnectionCallback[] = [];
  private disconnectionCallbacks: ConnectionCallback[] = [];
  private errorCallbacks: ErrorCallback[] = [];

  connect(token: string, userId?: number): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.client?.connected) {
        // If already connected and userId is provided, subscribe to user queue
        if (userId && !this.userQueueSubscription) {
          this.subscribeToUserQueue(userId);
        }
        resolve();
        return;
      }

      if (this.isConnecting) {
        this.onConnect(() => {
          if (userId && !this.userQueueSubscription) {
            this.subscribeToUserQueue(userId);
          }
          resolve();
        });
        return;
      }

      this.isConnecting = true;

      const socket = new SockJS(`${import.meta.env.VITE_WS_BASE_URL || 'http://localhost:8080'}/ws`);
      
      this.client = new Client({
        webSocketFactory: () => socket as any,
        connectHeaders: {
          Authorization: `Bearer ${token}`
        },
        debug: (str) => {
          if (import.meta.env.DEV) {
            console.log('[WebSocket Debug]', str);
          }
        },
        reconnectDelay: this.reconnectDelay,
        heartbeatIncoming: 30000,
        heartbeatOutgoing: 30000,
        onConnect: () => {
          console.log('[WebSocket] Connected successfully');
          this.isConnecting = false;
          this.reconnectAttempts = 0;
          
          // Automatically subscribe to user queue if userId is provided (for conversation list updates)
          if (userId && !this.userQueueSubscription) {
            this.subscribeToUserQueue(userId);
          }
          
          this.connectionCallbacks.forEach(callback => callback());
          this.connectionCallbacks = [];
          resolve();
        },
        onStompError: (frame) => {
          console.error('[WebSocket] STOMP error:', frame.headers['message']);
          console.error('[WebSocket] Error details:', frame.body);
          this.isConnecting = false;
          this.errorCallbacks.forEach(callback => callback(frame));
          reject(new Error(frame.headers['message'] || 'WebSocket connection failed'));
        },
        onWebSocketError: (event) => {
          console.error('[WebSocket] WebSocket error:', event);
          this.isConnecting = false;
          this.errorCallbacks.forEach(callback => callback(event));
        },
        onDisconnect: () => {
          console.log('[WebSocket] Disconnected');
          this.isConnecting = false;
          this.disconnectionCallbacks.forEach(callback => callback());
          this.handleReconnection(token, userId);
        }
      });

      this.client.activate();
    });
  }

  private handleReconnection(token: string, userId?: number): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
      
      console.log(`[WebSocket] Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts}) in ${delay}ms`);
      
      setTimeout(() => {
        this.connect(token, userId).catch(error => {
          console.error('[WebSocket] Reconnection failed:', error);
        });
      }, delay);
    } else {
      console.error('[WebSocket] Max reconnection attempts reached');
      this.reconnectAttempts = 0;
    }
  }

  subscribeToUserQueue(userId: number, callback?: MessageCallback): string | null {
    if (!this.client?.connected) {
      console.warn('[WebSocket] Cannot subscribe to user queue - not connected');
      return null;
    }

    // Unsubscribe from previous user queue if exists
    if (this.userQueueSubscription) {
      this.userQueueSubscription.unsubscribe();
      this.userQueueSubscription = null;
    }

    const destination = `/user/${userId}/queue/messages`;
    
    this.userQueueSubscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const parsedMessage = JSON.parse(message.body);
        console.log('[WebSocket] User queue message received (conversation list update):', parsedMessage);
        
        // Call the provided callback if available
        if (callback) {
          callback(parsedMessage);
        }
      } catch (error) {
        console.error('[WebSocket] Error parsing user queue message:', error);
        if (callback) {
          callback(message.body);
        }
      }
    });

    console.log(`[WebSocket] Subscribed to user queue for conversation list updates: ${destination}`);
    return 'user-queue-subscription';
  }

  subscribeToConversation(conversationId: string, callback: MessageCallback): string | null {
    if (!this.client?.connected) {
      console.warn('[WebSocket] Cannot subscribe to conversation - not connected');
      return null;
    }

    // Unsubscribe from previous conversation subscription if exists
    const existingSubscription = this.conversationSubscriptions.get(conversationId);
    if (existingSubscription) {
      existingSubscription.unsubscribe();
    }

    const destination = `/topic/conversation/${conversationId}`;
    
    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const parsedMessage = JSON.parse(message.body);
        console.log('[WebSocket] Conversation message received (active chat window):', parsedMessage);
        callback(parsedMessage);
      } catch (error) {
        console.error('[WebSocket] Error parsing conversation message:', error);
        callback(message.body);
      }
    });

    this.conversationSubscriptions.set(conversationId, subscription);
    console.log(`[WebSocket] Subscribed to conversation for active chat window: ${destination}`);
    
    return `conversation-${conversationId}`;
  }

  unsubscribeFromConversation(conversationId: string): void {
    const subscription = this.conversationSubscriptions.get(conversationId);
    if (subscription) {
      subscription.unsubscribe();
      this.conversationSubscriptions.delete(conversationId);
      console.log(`[WebSocket] Unsubscribed from conversation: ${conversationId}`);
    }
  }

  subscribe(destination: string, callback: MessageCallback): string {
    if (!this.client?.connected) {
      throw new Error('WebSocket is not connected. Call connect() first.');
    }

    const subscriptionId = `sub-${Date.now()}-${Math.random()}`;
    
    const subscription = this.client.subscribe(destination, (message: IMessage) => {
      try {
        const parsedMessage = JSON.parse(message.body);
        callback(parsedMessage);
      } catch (error) {
        console.error('[WebSocket] Error parsing message:', error);
        callback(message.body);
      }
    });

    this.subscriptions.set(subscriptionId, subscription);
    console.log(`[WebSocket] Subscribed to ${destination} with ID ${subscriptionId}`);
    
    return subscriptionId;
  }

  unsubscribe(subscriptionId: string): void {
    const subscription = this.subscriptions.get(subscriptionId);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(subscriptionId);
      console.log(`[WebSocket] Unsubscribed from ${subscriptionId}`);
    }
  }

  sendMessage(destination: string, body: any): void {
    if (!this.client?.connected) {
      throw new Error('WebSocket is not connected. Cannot send message.');
    }

    this.client.publish({
      destination,
      body: JSON.stringify(body)
    });

    console.log(`[WebSocket] Message sent to ${destination}`);
  }

  disconnect(): void {
    if (this.client) {
      // Unsubscribe from all regular subscriptions
      this.subscriptions.forEach((subscription) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();
      
      // Unsubscribe from all conversation subscriptions (multiple conversation support)
      this.conversationSubscriptions.forEach((subscription, conversationId) => {
        subscription.unsubscribe();
        console.log(`[WebSocket] Unsubscribed from conversation: ${conversationId}`);
      });
      this.conversationSubscriptions.clear();
      
      // Unsubscribe from user queue (conversation list updates)
      if (this.userQueueSubscription) {
        this.userQueueSubscription.unsubscribe();
        this.userQueueSubscription = null;
        console.log('[WebSocket] Unsubscribed from user queue');
      }
      
      this.client.deactivate();
      this.client = null;
      this.reconnectAttempts = 0;
      
      console.log('[WebSocket] Disconnected and cleaned up all subscriptions (dual subscriptions)');
    }
  }

  async reconnect(newToken: string, userId?: number): Promise<void> {
    console.log('[WebSocket] Reconnecting with new token...');
    
    // Store current conversation subscriptions to re-establish them
    const activeConversations = Array.from(this.conversationSubscriptions.keys());
    
    // Disconnect current connection
    this.disconnect();
    
    // Reset state
    this.isConnecting = false;
    this.reconnectAttempts = 0;
    
    // Connect with new token
    try {
      await this.connect(newToken, userId);
      console.log('[WebSocket] Reconnected successfully with new token');
      
      // Note: Conversation subscriptions will be re-established by components
      // User queue subscription is automatically established in connect()
      return Promise.resolve();
    } catch (error) {
      console.error('[WebSocket] Failed to reconnect with new token:', error);
      throw error;
    }
  }

  isConnected(): boolean {
    return this.client?.connected || false;
  }

  onConnect(callback: ConnectionCallback): void {
    if (this.client?.connected) {
      callback();
    } else {
      this.connectionCallbacks.push(callback);
    }
  }

  onDisconnect(callback: ConnectionCallback): void {
    this.disconnectionCallbacks.push(callback);
  }

  onError(callback: ErrorCallback): void {
    this.errorCallbacks.push(callback);
  }

  getActiveConversationSubscriptions(): string[] {
    return Array.from(this.conversationSubscriptions.keys());
  }

  hasUserQueueSubscription(): boolean {
    return this.userQueueSubscription !== null;
  }
}

export default new WebSocketService();
