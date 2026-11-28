export interface AdminCommentListItem {
  id: number;
  reportPublicId: string;
  reportId?: number;
  userId: number;
  username: string;
  userDisplayName: string;
  body: string;
  isHidden: boolean;
  deletedAt?: string | null;
  createdAt: string;
}

export interface AdminUpdateCommentStatusRequest {
  isHidden: boolean;
  reason: string;
}

export interface CommentFilterParams {
  reportPublicId?: string;
  isHidden?: boolean;
  page?: number;
  size?: number;
}
