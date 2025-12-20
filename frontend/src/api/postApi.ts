import axiosClient from './axiosClient';
import type { ApiResponse } from '@/types/dto/api-response.dto';
import type { PostDTO } from '@/types/dto/post.dto';
import type { CreatePostRequest, UpdatePostRequest } from '@/types/dto/request.dto';

export const postApi = {
  getAllPosts: async (): Promise<ApiResponse<PostDTO[]>> => {
    const response = await axiosClient.get<ApiResponse<PostDTO[]>>('/posts');
    return response.data;
  },

  getPostsByUserId: async (userId: number): Promise<ApiResponse<PostDTO[]>> => {
    const response = await axiosClient.get<ApiResponse<PostDTO[]>>(`/posts/user/${userId}`);
    return response.data;
  },

  getPostById: async (id: string): Promise<ApiResponse<PostDTO>> => {
    const response = await axiosClient.get<ApiResponse<PostDTO>>(`/posts/${id}`);
    return response.data;
  },

  createPost: async (data: CreatePostRequest): Promise<ApiResponse<PostDTO>> => {
    const response = await axiosClient.post<ApiResponse<PostDTO>>('/posts', data);
    return response.data;
  },

  updatePost: async (id: string, data: UpdatePostRequest): Promise<ApiResponse<PostDTO>> => {
    const response = await axiosClient.put<ApiResponse<PostDTO>>(`/posts/${id}`, data);
    return response.data;
  },

  deletePost: async (id: string): Promise<ApiResponse<void>> => {
    const response = await axiosClient.delete<ApiResponse<void>>(`/posts/${id}`);
    return response.data;
  },
};
