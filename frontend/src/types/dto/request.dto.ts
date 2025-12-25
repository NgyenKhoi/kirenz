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
  fullName: string;
  avatarUrl: string;
  bio: string;
  birthday: string;
}
