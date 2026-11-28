import { Role, UserStatus } from './common.types';

export interface AdminLoginRequest {
  identifier: string; // username or email
  password: string;
}

export interface AdminUser {
  id: number;
  username: string;
  email: string;
  displayName: string;
  avatarUrl?: string | null;
  role: Role;
  status: UserStatus;
}

export interface AdminAuthResponse {
  token: string;
  tokenType: string;
  user: AdminUser;
}
