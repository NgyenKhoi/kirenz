export interface MessageSummary {
  id: string;
  conversationId: string;
  senderId: number;
  senderName: string;
  type: 'TEXT' | 'IMAGE' | 'VIDEO' | 'SYSTEM';
  previewText: string; 
  sentAt: string;
}

export interface ConversationUpdateMessage {
  conversationId: string;
  conversationType: 'DIRECT' | 'GROUP';
  conversationName: string;
  lastMessage: MessageSummary;
  unreadCount: number;
  updatedAt: string;
  participants: Array<{
    userId: number;
    username: string;
    displayName: string;
  }>;
}