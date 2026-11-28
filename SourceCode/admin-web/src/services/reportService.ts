import { apiClient } from '@/api/apiClient';
import { mockService } from '@/mocks/mockService';
import { authService } from './authService';
import {
  AdminReportDetail,
  AdminReportSummary,
  AdminUpdateReportStatusRequest,
  ReportFilterParams
} from '@/types/report.types';
import { PageResponse } from '@/types/common.types';

const isMock = import.meta.env.VITE_USE_MOCK !== 'false';

export const reportService = {
  async getReports(params: ReportFilterParams): Promise<PageResponse<AdminReportSummary>> {
    if (isMock) {
      return mockService.getReports(params);
    }
    const res = await apiClient.get<{ success: boolean; data: PageResponse<AdminReportSummary> }>(
      '/api/v1/admin/reports',
      { params }
    );
    return res.data.data;
  },

  async getReportDetail(publicId: string): Promise<AdminReportDetail> {
    if (isMock) {
      return mockService.getReportDetail(publicId);
    }
    const res = await apiClient.get<{ success: boolean; data: AdminReportDetail }>(
      `/api/v1/admin/reports/${publicId}`
    );
    return res.data.data;
  },

  async updateReportStatus(publicId: string, request: AdminUpdateReportStatusRequest): Promise<AdminReportDetail> {
    if (isMock) {
      const user = authService.getCurrentUser();
      if (!user) throw new Error('Unauthenticated');
      return mockService.updateReportStatus(publicId, request, user);
    }
    const res = await apiClient.patch<{ success: boolean; data: AdminReportDetail }>(
      `/api/v1/admin/reports/${publicId}/status`,
      request
    );
    return res.data.data;
  }
};
