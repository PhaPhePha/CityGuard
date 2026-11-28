export interface AdminLogListItem {
  id: number;
  actorId: number;
  actorUsername: string;
  actorDisplayName?: string;
  action: string;
  targetType: 'USER' | 'REPORT' | 'COMMENT' | 'CATEGORY';
  targetId: number;
  reason?: string;
  createdAt: string;
}

export interface LogFilterParams {
  actorId?: number;
  action?: string;
  targetType?: string;
  page?: number;
  size?: number;
}
