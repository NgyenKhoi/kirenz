export interface SendMessageRequest {
  conversationId: string;
  content: string;
  media?: MediaUploadRequest[];
}

export interface MediaUploadRequest {
  type: 'IMAGE' | 'VIDEO';
  base64Data: string;
  fileName: string;
  fileSize: number;
}

export interface MessageResponse {
  id: string;
  conversationId: string;
  senderId: number;
  senderName: string;
  content: string;
  attachments: MediaAttachmentResponse[];
  type: 'TEXT' | 'IMAGE' | 'VIDEO' | 'SYSTEM';
  sentAt: string;
  statusList: MessageStatusResponse[];
}

export interface MediaAttachmentResponse {
  type: string;
  cloudinaryPublicId: string;
  url: string;
  metadata: Record<string, any>;
}

export interface MessageStatusResponse {
  userId: number;
  status: 'SENT' | 'DELIVERED' | 'READ';
  timestamp: string;
}

export interface ConversationResponse {
  id: string;
  type: 'DIRECT' | 'GROUP';
  name?: string;
  participants: ParticipantResponse[];
  lastMessage?: MessageResponse;
  unreadCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ParticipantResponse {
  userId: number;
  username: string;
  email: string;
  joinedAt: string;
}

export interface UserPresenceResponse {
  userId: number;
  username: string;
  status: 'ONLINE' | 'OFFLINE';
  lastSeen: string;
}

export interface CreateConversationRequest {
  type: 'DIRECT' | 'GROUP';
  name?: string;
  participantIds: number[];
}

export interface TypingIndicator {
  conversationId: string;
  userId: number;
  username: string;
  isTyping: boolean;
}

export interface ChatMessage {
  id?: string;
  conversationId: string;
  senderId: number;
  content: string;
  attachments?: MediaAttachmentResponse[];
  type: 'TEXT' | 'IMAGE' | 'VIDEO' | 'SYSTEM';
  sentAt?: string;
}

export interface MediaUploadResponse {
  cloudinaryPublicId: string;
  url: string;
  type: string;
  metadata: Record<string, any>;
}
