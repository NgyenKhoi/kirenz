import SockJS from 'sockjs-client';
import { Client, IMessage, StompSubscription } from '@stomp/stompjs';

type MessageCallback = (message: any) => void;
type ConnectionCallback = () => void;
type ErrorCallback = (error: any) => void;

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private reconnectDelay = 3000;
  private isConnecting = false;
  private connectionCallbacks: ConnectionCallback[] = [];
  private disconnectionCallbacks: ConnectionCallback[] = [];
  private errorCallbacks: ErrorCallback[] = [];

  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
      if (this.client?.connected) {
        resolve();
        return;
      }

      if (this.isConnecting) {
        this.onConnect(() => resolve());
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
          this.handleReconnection(token);
        }
      });

      this.client.activate();
    });
  }

  private handleReconnection(token: string): void {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      const delay = this.reconnectDelay * Math.pow(2, this.reconnectAttempts - 1);
      
      console.log(`[WebSocket] Attempting to reconnect (${this.reconnectAttempts}/${this.maxReconnectAttempts}) in ${delay}ms`);
      
      setTimeout(() => {
        this.connect(token).catch(error => {
          console.error('[WebSocket] Reconnection failed:', error);
        });
      }, delay);
    } else {
      console.error('[WebSocket] Max reconnection attempts reached');
      this.reconnectAttempts = 0;
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
      this.subscriptions.forEach((subscription) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();
      
      this.client.deactivate();
      this.client = null;
      this.reconnectAttempts = 0;
      
      console.log('[WebSocket] Disconnected and cleaned up');
    }
  }

  async reconnect(newToken: string): Promise<void> {
    console.log('[WebSocket] Reconnecting with new token...');
    
    // Store current subscriptions
    const currentSubscriptions = Array.from(this.subscriptions.entries());
    
    // Disconnect current connection
    this.disconnect();
    
    // Reset state
    this.isConnecting = false;
    this.reconnectAttempts = 0;
    
    // Connect with new token
    try {
      await this.connect(newToken);
      console.log('[WebSocket] Reconnected successfully with new token');
      
      // Note: Subscriptions will be re-established by the provider
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
}

export default new WebSocketService();
