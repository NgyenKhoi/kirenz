import axiosClient from './axiosClient';
import type { ApiResponse } from '@/types/dto/api-response.dto';
import type { CommentDTO } from '@/types/dto/comment.dto';
import type { CreateCommentRequest, UpdateCommentRequest } from '@/types/dto/request.dto';

export const commentApi = {
  getCommentsByPostId: async (postId: string): Promise<ApiResponse<CommentDTO[]>> => {
    const response = await axiosClient.get<ApiResponse<CommentDTO[]>>(`/comments/post/${postId}`);
    return response.data;
  },

  getCommentById: async (id: string): Promise<ApiResponse<CommentDTO>> => {
    const response = await axiosClient.get<ApiResponse<CommentDTO>>(`/comments/${id}`);
    return response.data;
  },

  createComment: async (data: CreateCommentRequest): Promise<ApiResponse<CommentDTO>> => {
    const response = await axiosClient.post<ApiResponse<CommentDTO>>('/comments', data);
    return response.data;
  },

  updateComment: async (id: string, data: UpdateCommentRequest): Promise<ApiResponse<CommentDTO>> => {
    const response = await axiosClient.put<ApiResponse<CommentDTO>>(`/comments/${id}`, data);
    return response.data;
  },

  deleteComment: async (id: string): Promise<ApiResponse<void>> => {
    const response = await axiosClient.delete<ApiResponse<void>>(`/comments/${id}`);
    return response.data;
  },
};
