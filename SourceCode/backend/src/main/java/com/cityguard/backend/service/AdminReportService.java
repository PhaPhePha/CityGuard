package com.cityguard.backend.service;

import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.dto.admin.report.AdminReportDetailResponse;
import com.cityguard.backend.dto.admin.report.AdminReportSummaryResponse;
import com.cityguard.backend.dto.admin.report.AdminUpdateReportStatusRequest;
import com.cityguard.backend.enums.ReportStatus;

public interface AdminReportService {
    AdminReportDetailResponse getReportDetail(String publicId);
    AdminPageResponse<AdminReportSummaryResponse> getReports(
            String keyword,
            Long categoryId,
            ReportStatus status,
            int page,
            int size,
            String sortBy,
            String direction
    );
    AdminReportDetailResponse updateReportStatus(
            String publicId,
            AdminUpdateReportStatusRequest request
    );
}
