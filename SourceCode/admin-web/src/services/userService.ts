import { apiClient } from '@/api/apiClient';
import { mockService } from '@/mocks/mockService';
import { authService } from './authService';
import {
  AdminUserDetail,
  AdminUserListItem,
  AdminUpdateUserStatusRequest,
  UserFilterParams
} from '@/types/user.types';
import { PageResponse } from '@/types/common.types';

const isMock = import.meta.env.VITE_USE_MOCK !== 'false';

export const userService = {
  async getUsers(params: UserFilterParams): Promise<PageResponse<AdminUserListItem>> {
    if (isMock) {
      return mockService.getUsers(params);
    }
    const res = await apiClient.get<{ success: boolean; data: PageResponse<AdminUserListItem> }>(
      '/api/v1/admin/users',
      { params }
    );
    return res.data.data;
  },

  async getUserDetail(id: number): Promise<AdminUserDetail> {
    if (isMock) {
      return mockService.getUserDetail(id);
    }
    const res = await apiClient.get<{ success: boolean; data: AdminUserDetail }>(
      `/api/v1/admin/users/${id}`
    );
    return res.data.data;
  },

  async updateUserStatus(id: number, request: AdminUpdateUserStatusRequest): Promise<AdminUserListItem> {
    if (isMock) {
      const user = authService.getCurrentUser();
      if (!user) throw new Error('Unauthenticated');
      return mockService.updateUserStatus(id, request, user);
    }
    const res = await apiClient.patch<{ success: boolean; data: AdminUserListItem }>(
      `/api/v1/admin/users/${id}/status`,
      request
    );
    return res.data.data;
  }
};
