import axiosClient from './axiosClient';
import type { ApiResponse } from '@/types/dto/api-response.dto';
import type {
  ConversationResponse,
  CreateConversationRequest,
  MessageResponse,
  UserPresenceResponse,
  MediaUploadRequest,
  MediaUploadResponse,
} from '@/types/dto/chat.dto';

export const chatApi = {
  createConversation: async (data: CreateConversationRequest): Promise<ApiResponse<ConversationResponse>> => {
    const response = await axiosClient.post<ApiResponse<ConversationResponse>>('/chat/conversations', data);
    return response.data;
  },

  getConversations: async (): Promise<ApiResponse<ConversationResponse[]>> => {
    const response = await axiosClient.get<ApiResponse<ConversationResponse[]>>('/chat/conversations');
    return response.data;
  },

  getMessages: async (
    conversationId: string,
    page: number = 0,
    size: number = 50
  ): Promise<ApiResponse<MessageResponse[]>> => {
    const response = await axiosClient.get<ApiResponse<MessageResponse[]>>(
      `/chat/conversations/${conversationId}/messages`,
      {
        params: { page, size },
      }
    );
    return response.data;
  },

  markAsRead: async (conversationId: string): Promise<ApiResponse<void>> => {
    const response = await axiosClient.post<ApiResponse<void>>(
      `/chat/conversations/${conversationId}/read`
    );
    return response.data;
  },

  getOnlineUsers: async (conversationId: string): Promise<ApiResponse<UserPresenceResponse[]>> => {
    const response = await axiosClient.get<ApiResponse<UserPresenceResponse[]>>(
      `/chat/conversations/${conversationId}/presence`
    );
    return response.data;
  },

  uploadMedia: async (data: MediaUploadRequest): Promise<ApiResponse<MediaUploadResponse>> => {
    const response = await axiosClient.post<ApiResponse<MediaUploadResponse>>('/media/upload', data);
    return response.data;
  },

  getAllUserPresence: async (): Promise<ApiResponse<UserPresenceResponse[]>> => {
    const response = await axiosClient.get<ApiResponse<UserPresenceResponse[]>>('/chat/presence');
    return response.data;
  },
};
