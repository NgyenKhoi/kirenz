import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { commentApi } from '@/api/commentApi';
import type { CreateCommentRequest, UpdateCommentRequest } from '@/types/dto/request.dto';
import { postKeys } from './usePostQueries';

export const commentKeys = {
  all: ['comments'] as const,
  
  lists: () => [...commentKeys.all, 'list'] as const,
  list: (postId: string) => [...commentKeys.lists(), 'post', postId] as const,

  details: () => [...commentKeys.all, 'detail'] as const,
  detail: (id: string) => [...commentKeys.details(), id] as const,
};

// Query hooks
export const usePostComments = (postId: string) => {
  return useQuery({
    queryKey: commentKeys.list(postId),
    queryFn: async () => {
      const response = await commentApi.getCommentsByPostId(postId);
      return response.result;
    },
    enabled: !!postId,
  });
};

export const useComment = (id: string) => {
  return useQuery({
    queryKey: commentKeys.detail(id),
    queryFn: async () => {
      const response = await commentApi.getCommentById(id);
      return response.result;
    },
    enabled: !!id,
  });
};

// Mutation hooks
export const useCreateComment = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateCommentRequest) => commentApi.createComment(data),
    onSuccess: (_response, variables) => {
      // Invalidate the specific post's comments list
      queryClient.invalidateQueries({ queryKey: commentKeys.list(variables.postId) });
      queryClient.invalidateQueries({ queryKey: postKeys.detail(variables.postId) });
    },
  });
};

export const useUpdateComment = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdateCommentRequest }) =>
      commentApi.updateComment(id, data),
    onSuccess: (_response, variables) => {
      // Invalidate the specific comment detail
      queryClient.invalidateQueries({ queryKey: commentKeys.detail(variables.id) });
      // Invalidate all comment lists to ensure consistency
      queryClient.invalidateQueries({ queryKey: commentKeys.lists() });
    },
  });
};

export const useDeleteComment = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => commentApi.deleteComment(id),
    onSuccess: (_response, id) => {
      // Remove the specific comment from cache
      queryClient.removeQueries({ queryKey: commentKeys.detail(id) });
      // Invalidate all comment lists to refresh UI
      queryClient.invalidateQueries({ queryKey: commentKeys.lists() });
    },
  });
};
