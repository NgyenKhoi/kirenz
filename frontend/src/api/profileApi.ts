import { ApiResponse, ProfileResponse, UpdateProfileRequest } from '../types/dto';
import { apiClient } from './client';

export const profileApi = {
  getProfile: (): Promise<ApiResponse<ProfileResponse>> => {
    return apiClient.get('/profile');
  },

  updateProfile: (data: UpdateProfileRequest): Promise<ApiResponse<ProfileResponse>> => {
    return apiClient.put('/profile', data);
  },

  deleteProfile: (): Promise<ApiResponse<void>> => {
    return apiClient.delete('/profile');
  }
};