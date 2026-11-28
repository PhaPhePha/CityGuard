import { apiClient } from '@/api/apiClient';
import { mockService } from '@/mocks/mockService';
import { AdminLogListItem, LogFilterParams } from '@/types/log.types';
import { PageResponse } from '@/types/common.types';

const isMock = import.meta.env.VITE_USE_MOCK !== 'false';

export const logService = {
  async getLogs(params: LogFilterParams): Promise<PageResponse<AdminLogListItem>> {
    if (isMock) {
      return mockService.getLogs(params);
    }
    const res = await apiClient.get<{ success: boolean; data: PageResponse<AdminLogListItem> }>(
      '/api/v1/admin/logs',
      { params }
    );
    return res.data.data;
  }
};
