import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { postApi } from '@/api/postApi';
import type { CreatePostRequest, UpdatePostRequest } from '@/types/dto/request.dto';

// Query keys factory - Centralized key management for type safety and easy invalidation
export const postKeys = {
  // Base key for all post-related queries
  all: ['posts'] as const,
  
  // List queries (multiple posts)
  lists: () => [...postKeys.all, 'list'] as const,
  list: (userId?: number) => userId 
    ? [...postKeys.lists(), 'user', userId] as const 
    : [...postKeys.lists(), 'all'] as const,
  
  // Detail queries (single post)
  details: () => [...postKeys.all, 'detail'] as const,
  detail: (id: string) => [...postKeys.details(), id] as const,
};

// Query hooks
export const useAllPosts = () => {
  return useQuery({
    queryKey: postKeys.list(),
    queryFn: async () => {
      const response = await postApi.getAllPosts();
      return response.result;
    },
  });
};

export const useUserPosts = (userId: number) => {
  return useQuery({
    queryKey: postKeys.list(userId),
    queryFn: async () => {
      const response = await postApi.getPostsByUserId(userId);
      return response.result;
    },
    enabled: !!userId,
  });
};

export const usePost = (id: string) => {
  return useQuery({
    queryKey: postKeys.detail(id),
    queryFn: async () => {
      const response = await postApi.getPostById(id);
      return response.result;
    },
    enabled: !!id,
  });
};

// Mutation hooks
export const useCreatePost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreatePostRequest) => postApi.createPost(data),
    onSuccess: (response, variables) => {
      // Invalidate all post lists (including all posts and user-specific posts)
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
};

export const useUpdatePost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: UpdatePostRequest }) =>
      postApi.updatePost(id, data),
    onSuccess: (_response, variables) => {
      // Invalidate the specific post detail
      queryClient.invalidateQueries({ queryKey: postKeys.detail(variables.id) });
      // Invalidate all post lists to ensure consistency
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
};

export const useDeletePost = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) => postApi.deletePost(id),
    onSuccess: (_response, id) => {
      // Remove the specific post from cache
      queryClient.removeQueries({ queryKey: postKeys.detail(id) });
      // Invalidate all post lists to refresh UI
      queryClient.invalidateQueries({ queryKey: postKeys.lists() });
    },
  });
};
