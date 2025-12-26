import { ProfileResponse } from './profile.dto';

export interface UserResponse {
  id: number;
  email: string;
  createdAt: string;
  updatedAt: string;
  profile?: ProfileResponse;
}