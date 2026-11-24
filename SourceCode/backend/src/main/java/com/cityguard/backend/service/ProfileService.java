package com.cityguard.backend.service;

import com.cityguard.backend.dto.common.PageResponse;
import com.cityguard.backend.dto.report.ReportResponse;
import com.cityguard.backend.dto.user.UserProfileSummaryResponse;
import com.cityguard.backend.enums.ReportStatus;

/**
 * Interface nghiệp vụ quản lý thông tin hồ sơ và báo cáo cá nhân (WBS Task 4.6.1).
 */
public interface ProfileService {
    PageResponse<ReportResponse> getMyReports(ReportStatus status, int page, int size);
    UserProfileSummaryResponse getProfileSummary();
}

