import type { ProfileDTO } from './profile.dto';

export interface UserDTO {
  id: number;
  email: string;
  createdAt: string;
  updatedAt: string;
  profile?: ProfileDTO;
}
