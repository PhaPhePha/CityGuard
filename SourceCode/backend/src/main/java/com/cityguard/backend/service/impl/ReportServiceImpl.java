package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.request.CommentRequest;
import com.cityguard.backend.dto.request.CreateReportRequest;
import com.cityguard.backend.dto.request.VoteRequest;
import com.cityguard.backend.dto.response.ReportResponse;
import com.cityguard.backend.entity.Report;
import com.cityguard.backend.entity.ReportMetrics;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.enums.ReportType;
import com.cityguard.backend.enums.TrustLabel;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.ReportRepository;
import com.cityguard.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai ReportService kết nối cơ sở dữ liệu PostgreSQL thực tế (theo WBS Task 4.3.1).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;

    @Override
    public List<ReportResponse> getReports(String bbox, ReportType type, ReportStatus status) {
        List<ReportStatus> statuses;
        if (status != null) {
            statuses = List.of(status);
        } else {
            // Mặc định lọc các trạng thái còn hiển thị trên bản đồ (theo DacTaKyThuat Trang 4 Mục 2.2)
            statuses = List.of(ReportStatus.PENDING_VERIFY, ReportStatus.ACTIVE);
        }

        String categoryCode = type != null ? type.name() : null;

        Double minLng = null, minLat = null, maxLng = null, maxLat = null;
        if (bbox != null && !bbox.isBlank()) {
            String[] parts = bbox.split(",");
            if (parts.length == 4) {
                try {
                    double p0 = Double.parseDouble(parts[0].trim());
                    double p1 = Double.parseDouble(parts[1].trim());
                    double p2 = Double.parseDouble(parts[2].trim());
                    double p3 = Double.parseDouble(parts[3].trim());

                    if (p0 > p1) {
                        minLng = Math.min(p0, p2);
                        maxLng = Math.max(p0, p2);
                        minLat = Math.min(p1, p3);
                        maxLat = Math.max(p1, p3);
                    } else {
                        minLat = Math.min(p0, p2);
                        maxLat = Math.max(p0, p2);
                        minLng = Math.min(p1, p3);
                        maxLng = Math.max(p1, p3);
                    }
                } catch (NumberFormatException e) {
                    log.warn("Không thể phân tích tham số bounding box: {}", bbox);
                }
            }
        }

        List<Report> reports = reportRepository.findReports(
                statuses,
                categoryCode,
                OffsetDateTime.now(),
                minLat,
                maxLat,
                minLng,
                maxLng
        );

        return reports.stream()
                .map(this::mapToReportResponse)
                .toList();
    }

    @Override
    public ReportResponse getReportById(String id) {
        if (id == null || id.isBlank()) {
            throw new AppException(ErrorCode.REPORT_NOT_FOUND);
        }

        String trimmedId = id.trim();
        Optional<Report> reportOpt;

        // Hỗ trợ tìm kiếm theo số ID (Long) nếu chuỗi là số nguyên, hoặc theo public_id ("rpt_...")
        if (trimmedId.matches("^\\d+$")) {
            Long numericId = Long.parseLong(trimmedId);
            reportOpt = reportRepository.findByIdWithDetails(numericId)
                    .or(() -> reportRepository.findByPublicIdWithDetails(trimmedId));
        } else {
            reportOpt = reportRepository.findByPublicIdWithDetails(trimmedId);
        }

        return reportOpt.map(this::mapToReportResponse)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
    }

    private ReportResponse mapToReportResponse(Report report) {
        ReportType type = null;
        if (report.getCategory() != null && report.getCategory().getCode() != null) {
            try {
                type = ReportType.valueOf(report.getCategory().getCode());
            } catch (IllegalArgumentException e) {
                log.warn("Mã category không khớp ReportType enum: {}", report.getCategory().getCode());
            }
        }

        ReportMetrics metrics = report.getReportMetrics();
        Integer trustScore = null;
        TrustLabel trustLabel = TrustLabel.PENDING_VERIFY;
        int confirmCount = 0;
        int noLongerCount = 0;

        if (metrics != null) {
            if (metrics.getConfirmCount() != null) {
                confirmCount = metrics.getConfirmCount().intValue();
            }
            if (metrics.getNoLongerCount() != null) {
                noLongerCount = metrics.getNoLongerCount().intValue();
            }
            if (metrics.getTrustScore() != null) {
                trustScore = metrics.getTrustScore().intValue();
            }
            if (metrics.getTrustLabel() != null) {
                try {
                    trustLabel = TrustLabel.valueOf(metrics.getTrustLabel());
                } catch (IllegalArgumentException e) {
                    trustLabel = TrustLabel.PENDING_VERIFY;
                }
            }
        }

        String createdAtStr = report.getCreatedAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(report.getCreatedAt().toInstant())
                : null;
        String expiresAtStr = report.getExpiresAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(report.getExpiresAt().toInstant())
                : null;
        String creatorId = report.getUser() != null
                ? String.valueOf(report.getUser().getId())
                : null;

        return ReportResponse.builder()
                .id(report.getPublicId())
                .type(type)
                .status(report.getStatus())
                .trustScore(trustScore)
                .trustLabel(trustLabel)
                .confirmCount(confirmCount)
                .noLongerCount(noLongerCount)
                .userVote(null)
                .latitude(report.getLatitude())
                .longitude(report.getLongitude())
                .addressSnapshot(report.getAddressSnapshot())
                .description(report.getDescription())
                .imageUrl(report.getImageUrl())
                .createdAt(createdAtStr)
                .expiresAt(expiresAtStr)
                .creatorId(creatorId)
                .comments(List.of())
                .build();
    }

    @Override
    public ReportResponse create(String userId, CreateReportRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'create'");
    }

    @Override
    public ReportResponse vote(String userId, String id, VoteRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'vote'");
    }

    @Override
    public ReportResponse comment(String userId, String id, CommentRequest request) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'comment'");
    }

    @Override
    public List<ReportResponse> getMyReports(String userId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'getMyReports'");
    }
}
