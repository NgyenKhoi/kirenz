# Chat Components

This directory contains all the React components for the real-time chat feature.

## Components

### ChatWindow
The main chat interface component that displays messages and handles message sending.

**Props:**
- `conversation: ConversationResponse` - The conversation to display
- `currentUserId: number` - The ID of the current user
- `onBack?: () => void` - Optional callback for back navigation (mobile)
- `className?: string` - Optional CSS classes

**Features:**
- Real-time message display
- WebSocket integration for live updates
- Typing indicators
- Message pagination with infinite scroll
- Media attachment support
- Presence indicators for direct messages

### ConversationList
Displays a list of user's conversations with preview and unread counts.

**Props:**
- `conversations: ConversationResponse[]` - Array of conversations
- `activeConversationId: string | null` - Currently selected conversation ID
- `currentUserId: number` - The ID of the current user
- `onlineUsers: Map<number, boolean>` - Map of user IDs to online status
- `onSelectConversation: (conversationId: string) => void` - Callback when conversation is selected
- `isLoading?: boolean` - Loading state
- `className?: string` - Optional CSS classes

**Features:**
- Conversation preview with last message
- Unread message badges
- Online/offline indicators for direct messages
- Responsive design
- Loading skeletons

### MessageList
Displays messages in a conversation with infinite scroll pagination.

**Props:**
- `messages: MessageResponse[]` - Array of messages to display
- `currentUserId: number` - The ID of the current user
- `isLoading?: boolean` - Loading state
- `hasMore?: boolean` - Whether more messages can be loaded
- `onLoadMore?: () => void` - Callback to load more messages
- `className?: string` - Optional CSS classes

**Features:**
- Infinite scroll pagination
- Auto-scroll to bottom for new messages
- Message grouping by sender
- Media attachment display (images/videos)
- Timestamp formatting
- Loading skeletons

### MessageInput
Input component for composing and sending messages.

**Props:**
- `onSendMessage: (content: string, media?: MediaUploadRequest[]) => void` - Callback when message is sent
- `onTyping?: () => void` - Optional callback when user is typing
- `disabled?: boolean` - Disable input
- `placeholder?: string` - Input placeholder text

**Features:**
- Text input with auto-resize
- Image and video attachment support
- File size validation
- Base64 encoding for media
- Keyboard shortcuts (Enter to send, Shift+Enter for new line)
- Typing indicator emission

### PresenceIndicator
Visual indicator for user online/offline status.

**Props:**
- `isOnline: boolean` - Whether the user is online
- `size?: 'sm' | 'md' | 'lg'` - Size of the indicator
- `showLabel?: boolean` - Whether to show "Online"/"Offline" label
- `className?: string` - Optional CSS classes

**Features:**
- Animated pulse effect when online
- Configurable sizes
- Optional text label

### TypingIndicator
Animated indicator showing when someone is typing.

**Props:**
- `username?: string` - Optional username to display
- `className?: string` - Optional CSS classes

**Features:**
- Animated dots
- Optional username display
- Smooth animations

## Usage Example

```tsx
import { ChatWindow, ConversationList } from '@/components/chat';
import { useChatStore } from '@/stores/chatStore';

function ChatPage() {
  const { conversations, activeConversation, onlineUsers } = useChatStore();
  const currentUserId = 1; // Get from auth

  const selectedConversation = conversations.find(
    c => c.id === activeConversation
  );

  return (
    <div className="grid grid-cols-3 gap-4">
      <div className="col-span-1">
        <ConversationList
          conversations={conversations}
          activeConversationId={activeConversation}
          currentUserId={currentUserId}
          onlineUsers={onlineUsers}
          onSelectConversation={(id) => setActiveConversation(id)}
        />
      </div>
      
      <div className="col-span-2">
        {selectedConversation && (
          <ChatWindow
            conversation={selectedConversation}
            currentUserId={currentUserId}
          />
        )}
      </div>
    </div>
  );
}
```

## WebSocket Integration

The components integrate with the WebSocket service for real-time updates:

- **Message delivery**: Subscribe to `/topic/conversation/{conversationId}`
- **Typing indicators**: Subscribe to `/topic/conversation/{conversationId}/typing`
- **Presence updates**: Subscribe to `/user/queue/presence`

## State Management

The components use Zustand store (`useChatStore`) for state management:

- `conversations` - List of user's conversations
- `activeConversation` - Currently selected conversation ID
- `messages` - Map of conversation IDs to message arrays
- `onlineUsers` - Map of user IDs to online status

## Styling

All components use Tailwind CSS and shadcn/ui components for consistent styling:

- Responsive design with mobile-first approach
- Dark mode support
- Smooth animations and transitions
- Accessible components

## Requirements Validation

These components satisfy the following requirements:

- **1.1**: Direct message conversation creation and display
- **1.5**: Real-time message delivery via WebSocket
- **3.4**: Online status display for participants
- **4.1**: Image attachment support in messages
- **9.1**: Message history retrieval with pagination
