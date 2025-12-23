export interface ProfileDTO {
  id: number;
  fullName: string;
  avatarUrl: string;
  bio: string;
  birthday: string;
  updatedAt: string;
}

export interface ProfileResponse {
  id: number;
  fullName?: string;
  bio?: string;
  location?: string;
  website?: string;
  dateOfBirth?: string;
  avatarUrl?: string;
  createdAt?: string;
  updatedAt: string;
}

export interface UpdateProfileRequest {
  fullName?: string;
  bio?: string;
  location?: string;
  website?: string;
  dateOfBirth?: string;
}
