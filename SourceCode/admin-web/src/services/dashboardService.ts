import { apiClient } from '@/api/apiClient';
import { mockService } from '@/mocks/mockService';
import { AdminDashboardSummary } from '@/types/dashboard.types';

const isMock = import.meta.env.VITE_USE_MOCK !== 'false';

export const dashboardService = {
  async getSummary(): Promise<AdminDashboardSummary> {
    if (isMock) {
      return mockService.getDashboardSummary();
    }
    const res = await apiClient.get<{ success: boolean; data: AdminDashboardSummary }>(
      '/api/v1/admin/dashboard/summary'
    );
    return res.data.data;
  }
};
