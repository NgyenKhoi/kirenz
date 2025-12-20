import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { userApi } from '@/api/userApi';
import type { UpdateProfileRequest } from '@/types/dto/request.dto';

// Query keys
export const userKeys = {
  all: ['users'] as const,
  lists: () => [...userKeys.all, 'list'] as const,
  list: () => [...userKeys.lists()] as const,
  details: () => [...userKeys.all, 'detail'] as const,
  detail: (id: number) => [...userKeys.details(), id] as const,
  profiles: () => [...userKeys.all, 'profile'] as const,
  profile: (id: number) => [...userKeys.profiles(), id] as const,
};

// Query hooks
export const useUsers = () => {
  return useQuery({
    queryKey: userKeys.list(),
    queryFn: async () => {
      const response = await userApi.getUsers();
      return response.result;
    },
  });
};

export const useUser = (id: number) => {
  return useQuery({
    queryKey: userKeys.detail(id),
    queryFn: async () => {
      const response = await userApi.getUserById(id);
      return response.result;
    },
    enabled: !!id,
  });
};

export const useUserProfile = (id: number, options?: { staleTime?: number }) => {
  return useQuery({
    queryKey: userKeys.profile(id),
    queryFn: async () => {
      const response = await userApi.getUserProfile(id);
      return response.result;
    },
    enabled: !!id,
    staleTime: options?.staleTime || 0,
  });
};

// Mutation hooks
export const useUpdateProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateProfileRequest }) =>
      userApi.updateProfile(id, data),
    onSuccess: (response, variables) => {
      // Invalidate and refetch user profile queries
      queryClient.invalidateQueries({ queryKey: userKeys.profile(variables.id) });
      queryClient.invalidateQueries({ queryKey: userKeys.detail(variables.id) });
    },
  });
};
