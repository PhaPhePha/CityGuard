export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data: T;
  timestamp?: string;
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  isLast: boolean;
}

export type Role = 'ADMIN' | 'MODERATOR' | 'USER';
export type UserStatus = 'ACTIVE' | 'LOCKED';
export type ReportStatus = 'PENDING_VERIFY' | 'ACTIVE' | 'EXPIRED' | 'RESOLVED' | 'HIDDEN';
