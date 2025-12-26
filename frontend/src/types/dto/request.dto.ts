import { MediaItemDTO } from './post.dto';

export interface CreatePostRequest {
  content: string;
  media: MediaItemDTO[];
}

export interface UpdatePostRequest {
  content: string;
  media: MediaItemDTO[];
}

export interface CreateCommentRequest {
  postId: string;
  content: string;
}

export interface UpdateCommentRequest {
  content: string;
}

export interface UpdateProfileRequest {
  fullName?: string;
  bio?: string;
  location?: string;
  website?: string;
  dateOfBirth?: string;
  avatarUrl?: string;
}
