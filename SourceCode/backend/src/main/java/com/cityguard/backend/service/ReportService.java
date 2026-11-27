package com.cityguard.backend.service;

import com.cityguard.backend.dto.report.CreateReportRequest;
import com.cityguard.backend.dto.report.ReportResponse;
import com.cityguard.backend.dto.report.VoteRequest;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.enums.ReportType;

import java.util.List;

/**
 * Interface nghiệp vụ xử lý dữ liệu báo cáo sự cố (theo NFR-MAINT-01 & WBS Task 4.3.1).
 */
public interface ReportService {
    List<ReportResponse> getReports(String bbox, ReportType type, ReportStatus status, String sort, Double userLat, Double userLng);
    ReportResponse getReportById(String id);
    ReportResponse create(CreateReportRequest request);
    ReportResponse vote(String id, VoteRequest request);
    List<ReportResponse> getMyReports();
}
