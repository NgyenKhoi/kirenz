import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { chatApi } from '@/api/chatApi';
import type { CreateConversationRequest } from '@/types/dto/chat.dto';
import { toast } from 'sonner';

// Query keys
export const chatKeys = {
  all: ['chat'] as const,
  conversations: () => [...chatKeys.all, 'conversations'] as const,
  conversation: (id: string) => [...chatKeys.all, 'conversation', id] as const,
  messages: (conversationId: string, page?: number) => 
    [...chatKeys.all, 'messages', conversationId, page] as const,
  presence: (conversationId: string) => 
    [...chatKeys.all, 'presence', conversationId] as const,
  allPresence: () => [...chatKeys.all, 'presence', 'all'] as const,
};

// Conversations query
export const useConversations = () => {
  return useQuery({
    queryKey: chatKeys.conversations(),
    queryFn: async () => {
      const response = await chatApi.getConversations();
      return response.result || [];
    },
    staleTime: 30000, // 30 seconds
    gcTime: 300000, // 5 minutes
    refetchOnWindowFocus: false,
    refetchOnMount: false,
  });
};

// Messages query
export const useMessages = (conversationId: string, page: number = 0, size: number = 50) => {
  return useQuery({
    queryKey: chatKeys.messages(conversationId, page),
    queryFn: async () => {
      const response = await chatApi.getMessages(conversationId, page, size);
      return response.result || [];
    },
    enabled: !!conversationId,
    staleTime: 30000, // 30 seconds
    gcTime: 300000, // 5 minutes
    refetchOnWindowFocus: false,
    refetchOnMount: false,
  });
};

// Create conversation mutation
export const useCreateConversation = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateConversationRequest) => chatApi.createConversation(data),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: chatKeys.conversations() });
      toast.success('Conversation created');
      return response.result;
    },
    onError: (error: any) => {
      toast.error(error.response?.data?.message || 'Failed to create conversation');
    },
  });
};

// Mark as read mutation
export const useMarkAsRead = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (conversationId: string) => chatApi.markAsRead(conversationId),
    onSuccess: (_, conversationId) => {
      // Invalidate conversations to update unread count
      queryClient.invalidateQueries({ queryKey: chatKeys.conversations() });
    },
    onError: (error: any) => {
      console.error('Failed to mark as read:', error);
      // Don't show error to user, it's not critical
    },
    retry: 1, // Only retry once
  });
};

// Get online users query
export const useOnlineUsers = (conversationId: string) => {
  return useQuery({
    queryKey: chatKeys.presence(conversationId),
    queryFn: async () => {
      const response = await chatApi.getOnlineUsers(conversationId);
      return response.result || [];
    },
    enabled: !!conversationId,
    refetchInterval: 30000, // Refresh every 30 seconds
  });
};

// Get all user presence query
export const useAllUserPresence = (enabled: boolean = true) => {
  return useQuery({
    queryKey: chatKeys.allPresence(),
    queryFn: async () => {
      const response = await chatApi.getAllUserPresence();
      return response.result || [];
    },
    enabled,
    staleTime: 10000, // 10 seconds
    refetchInterval: 30000, // Refresh every 30 seconds
  });
};
