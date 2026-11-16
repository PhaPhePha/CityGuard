package com.cityguard.backend.service;

import com.cityguard.backend.dto.response.ReportResponse;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.enums.ReportType;

import java.util.List;

/**
 * Interface nghiệp vụ xử lý dữ liệu báo cáo sự cố.
 */
public interface ReportService {
    List<ReportResponse> getReports(String bbox, ReportType type, ReportStatus status);
    ReportResponse getReportById(String id);
}

