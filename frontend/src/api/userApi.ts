import axiosClient from './axiosClient';
import type { ApiResponse } from '@/types/dto/api-response.dto';
import type { UserDTO } from '@/types/dto/user.dto';
import type { ProfileDTO } from '@/types/dto/profile.dto';
import type { UpdateProfileRequest } from '@/types/dto/request.dto';

export const userApi = {
  getUsers: async (): Promise<ApiResponse<UserDTO[]>> => {
    const response = await axiosClient.get<ApiResponse<UserDTO[]>>('/users');
    return response.data;
  },

  getUserById: async (id: number): Promise<ApiResponse<UserDTO>> => {
    const response = await axiosClient.get<ApiResponse<UserDTO>>(`/users/${id}`);
    return response.data;
  },

  getUserProfile: async (id: number): Promise<ApiResponse<UserDTO>> => {
    const response = await axiosClient.get<ApiResponse<UserDTO>>(`/users/${id}/profile`);
    return response.data;
  },

  updateProfile: async (id: number, data: UpdateProfileRequest): Promise<ApiResponse<ProfileDTO>> => {
    const response = await axiosClient.put<ApiResponse<ProfileDTO>>(`/users/${id}/profile`, data);
    return response.data;
  },
};
