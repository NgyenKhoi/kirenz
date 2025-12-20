import { MediaItemDTO } from './post.dto';

export interface CreatePostRequest {
  // userId is obtained from JWT token on backend
  content: string;
  media: MediaItemDTO[];
}

export interface UpdatePostRequest {
  content: string;
  media: MediaItemDTO[];
}

export interface CreateCommentRequest {
  postId: string;
  // userId is obtained from JWT token on backend
  content: string;
}

export interface UpdateCommentRequest {
  content: string;
}

export interface UpdateProfileRequest {
  fullName: string;
  avatarUrl: string;
  bio: string;
  birthday: string;
}
