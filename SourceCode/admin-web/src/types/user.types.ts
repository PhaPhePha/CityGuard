import { Role, UserStatus } from './common.types';
import { AdminReportSummary } from './report.types';

export type { Role, Role as UserRole, UserStatus };

export interface AdminUserListItem {
  id: number;
  username: string;
  email: string;
  displayName: string;
  avatarUrl?: string | null;
  role: Role;
  status: UserStatus;
  createdAt: string;
}

export interface AdminUserDetail extends AdminUserListItem {
  totalReports: number;
  recentReports: AdminReportSummary[];
}

export interface AdminUpdateUserStatusRequest {
  status: UserStatus;
  reason: string;
}

export interface UserFilterParams {
  search?: string;
  role?: Role;
  status?: UserStatus;
  page?: number;
  size?: number;
}
