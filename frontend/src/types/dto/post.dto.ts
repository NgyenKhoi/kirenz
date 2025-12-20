export interface MediaItemDTO {
  type: string;
  url: string;
}

export interface PostAuthorDTO {
  id: number;
  email: string;
  fullName?: string;
  avatarUrl?: string;
}

export interface PostDTO {
  id: string;
  slug: string;
  userId: number;
  author?: PostAuthorDTO; // Optional for backward compatibility
  content: string;
  media: MediaItemDTO[];
  createdAt: string;
  updatedAt: string;
  likes: number;
  commentsCount: number;
}
