import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { profileApi } from '@/api/profileApi';
import { userApi } from '@/api/userApi';
import type { UpdateProfileRequest } from '@/types/dto/request.dto';

export const profileKeys = {
  all: ['profiles'] as const,
  details: () => [...profileKeys.all, 'detail'] as const,
  detail: (id?: number) => [...profileKeys.details(), id] as const,
  own: () => [...profileKeys.all, 'own'] as const,
};

export const useOwnProfile = () => {
  return useQuery({
    queryKey: profileKeys.own(),
    queryFn: async () => {
      const response = await profileApi.getProfile();
      return response.result;
    },
  });
};

export const useUserProfile = (userId: string, options?: { enabled?: boolean }) => {
  return useQuery({
    queryKey: profileKeys.detail(parseInt(userId)),
    queryFn: async () => {
      const response = await profileApi.getUserProfile(userId);
      return response.result;
    },
    enabled: !!userId && (options?.enabled !== false),
  });
};

export const useUpdateProfile = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: UpdateProfileRequest) => profileApi.updateProfile(data),
    onSuccess: (response) => {
      queryClient.invalidateQueries({ queryKey: profileKeys.own() });
      queryClient.setQueryData(profileKeys.own(), response.result);
    },
  });
};