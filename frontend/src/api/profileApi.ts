import axiosClient from './axiosClient';
import type { ApiResponse } from '@/types/dto/api-response.dto';
import type { ProfileResponse } from '@/types/dto/profile.dto';
import type { UpdateProfileRequest } from '@/types/dto/request.dto';

export const profileApi = {
  getProfile: async (): Promise<ApiResponse<ProfileResponse>> => {
    const response = await axiosClient.get<ApiResponse<ProfileResponse>>('/profile');
    return response.data;
  },

  updateProfile: async (data: UpdateProfileRequest): Promise<ApiResponse<ProfileResponse>> => {
    const response = await axiosClient.put<ApiResponse<ProfileResponse>>('/profile', data);
    return response.data;
  },

  deleteProfile: async (): Promise<ApiResponse<void>> => {
    const response = await axiosClient.delete<ApiResponse<void>>('/profile');
    return response.data;
  },
};