import { apiClient } from '@/api/apiClient';
import { mockService } from '@/mocks/mockService';
import { authService } from './authService';
import {
  AdminCommentListItem,
  AdminUpdateCommentStatusRequest,
  CommentFilterParams
} from '@/types/comment.types';
import { PageResponse } from '@/types/common.types';

const isMock = import.meta.env.VITE_USE_MOCK !== 'false';

export const commentService = {
  async getComments(params: CommentFilterParams): Promise<PageResponse<AdminCommentListItem>> {
    if (isMock) {
      return mockService.getComments(params);
    }
    const res = await apiClient.get<{ success: boolean; data: PageResponse<AdminCommentListItem> }>(
      '/api/v1/admin/comments',
      { params }
    );
    return res.data.data;
  },

  async updateCommentStatus(id: number, request: AdminUpdateCommentStatusRequest): Promise<AdminCommentListItem> {
    if (isMock) {
      const user = authService.getCurrentUser();
      if (!user) throw new Error('Unauthenticated');
      return mockService.updateCommentStatus(id, request, user);
    }
    const res = await apiClient.patch<{ success: boolean; data: AdminCommentListItem }>(
      `/api/v1/admin/comments/${id}/status`,
      request
    );
    return res.data.data;
  }
};
