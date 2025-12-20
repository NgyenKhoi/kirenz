# WebSocket Service

This service provides real-time communication capabilities using SockJS and STOMP protocol.

## Features

- JWT token authentication
- Automatic reconnection with exponential backoff
- Subscription management
- Connection state tracking
- Error handling
- Heartbeat support

## Usage

### Basic Usage with Hook

```typescript
import { useWebSocket } from '../hooks/useWebSocket';

function ChatComponent() {
  const { isConnected, connect, subscribe, sendMessage } = useWebSocket({
    autoConnect: true,
    onConnect: () => console.log('Connected!'),
    onDisconnect: () => console.log('Disconnected!'),
    onError: (error) => console.error('Error:', error)
  });

  useEffect(() => {
    if (isConnected) {
      const subscriptionId = subscribe('/topic/conversation/123', (message) => {
        console.log('Received message:', message);
      });

      return () => {
        unsubscribe(subscriptionId);
      };
    }
  }, [isConnected]);

  const handleSendMessage = () => {
    sendMessage('/app/chat.send', {
      conversationId: '123',
      content: 'Hello!'
    });
  };

  return (
    <div>
      <p>Status: {isConnected ? 'Connected' : 'Disconnected'}</p>
      <button onClick={handleSendMessage}>Send Message</button>
    </div>
  );
}
```

### Direct Service Usage

```typescript
import { websocketService } from '../services';
import { useAuthStore } from '../stores/authStore';

// Connect
const token = useAuthStore.getState().accessToken;
await websocketService.connect(token);

// Subscribe to a topic
const subscriptionId = websocketService.subscribe(
  '/topic/conversation/123',
  (message) => {
    console.log('Received:', message);
  }
);

// Send a message
websocketService.sendMessage('/app/chat.send', {
  conversationId: '123',
  content: 'Hello World!'
});

// Unsubscribe
websocketService.unsubscribe(subscriptionId);

// Disconnect
websocketService.disconnect();
```

## WebSocket Endpoints

### Subscribe Destinations

- `/topic/conversation/{conversationId}` - Subscribe to conversation messages
- `/topic/conversation/{conversationId}/typing` - Subscribe to typing indicators
- `/user/queue/presence` - Subscribe to user presence updates

### Send Destinations

- `/app/chat.send` - Send a message
- `/app/chat.typing` - Send typing indicator

## Configuration

Set the WebSocket base URL in your `.env` file:

```
VITE_WS_BASE_URL=http://localhost:8080
```

## Reconnection Strategy

The service automatically attempts to reconnect with exponential backoff:

- Max attempts: 5
- Initial delay: 3 seconds
- Backoff multiplier: 2x
- Max delay: 48 seconds (3s * 2^4)

## Error Handling

The service provides error callbacks for:

- STOMP protocol errors
- WebSocket connection errors
- Message parsing errors

## Heartbeat

The service maintains connection health with:

- Incoming heartbeat: 30 seconds
- Outgoing heartbeat: 30 seconds
