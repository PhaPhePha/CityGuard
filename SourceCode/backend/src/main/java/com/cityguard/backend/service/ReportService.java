package com.cityguard.backend.service;

import com.cityguard.backend.dto.response.ReportResponse;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.enums.ReportType;

import java.util.List;
import com.cityguard.backend.dto.request.CommentRequest;
import com.cityguard.backend.dto.request.CreateReportRequest;
import com.cityguard.backend.dto.request.VoteRequest;

/**
 * Interface nghiệp vụ xử lý dữ liệu báo cáo sự cố.
 */
public interface ReportService {
    List<ReportResponse> getReports(String bbox, ReportType type, ReportStatus status);
    ReportResponse getReportById(String id);
    ReportResponse create(String userId, CreateReportRequest request);
    ReportResponse vote(String userId, String id, VoteRequest request);
    ReportResponse comment(String userId, String id, CommentRequest request);
    List<ReportResponse> getMyReports(String userId);
}

