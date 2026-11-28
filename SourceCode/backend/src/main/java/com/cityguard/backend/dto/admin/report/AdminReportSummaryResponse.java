package com.cityguard.backend.dto.admin.report;

import com.cityguard.backend.entity.Category;
import com.cityguard.backend.entity.Report;
import com.cityguard.backend.entity.ReportMetrics;
import com.cityguard.backend.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * DTO tóm tắt báo cáo sự cố trong danh sách Admin (GET /api/v1/admin/reports).
 * Tuyệt đối chỉ dùng publicId (rpt_<ULID>), không làm lộ khóa chính nội bộ reports.id (BIGINT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportSummaryResponse {

    private String publicId;
    private Long categoryId;
    private String categoryCode;
    private String categoryDisplayName;
    private String categoryColor;
    private String description;
    private String imageUrl;
    private Double latitude;
    private Double longitude;
    private String addressSnapshot;
    private ReportStatus status;
    private long confirmCount;
    private long noLongerCount;
    private BigDecimal trustScore;
    private String trustLabel;
    private String createdAt;
    private String expiresAt;

    public static AdminReportSummaryResponse from(Report report) {
        if (report == null) {
            return null;
        }
        Category category = report.getCategory();
        ReportMetrics metrics = report.getReportMetrics();

        long confirmCount = metrics != null && metrics.getConfirmCount() != null
                ? metrics.getConfirmCount()
                : 0L;
        long noLongerCount = metrics != null && metrics.getNoLongerCount() != null
                ? metrics.getNoLongerCount()
                : 0L;
        BigDecimal trustScore = metrics != null ? metrics.getTrustScore() : null;
        String trustLabel = metrics != null ? metrics.getTrustLabel() : null;

        String createdAt = report.getCreatedAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(report.getCreatedAt().toInstant())
                : null;
        String expiresAt = report.getExpiresAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(report.getExpiresAt().toInstant())
                : null;

        return AdminReportSummaryResponse.builder()
                .publicId(report.getPublicId())
                .categoryId(category != null ? category.getId() : null)
                .categoryCode(category != null ? category.getCode() : null)
                .categoryDisplayName(category != null ? category.getDisplayName() : null)
                .categoryColor(category != null ? category.getColor() : null)
                .description(report.getDescription())
                .imageUrl(report.getImageUrl())
                .latitude(report.getLatitude())
                .longitude(report.getLongitude())
                .addressSnapshot(report.getAddressSnapshot())
                .status(report.getStatus())
                .confirmCount(confirmCount)
                .noLongerCount(noLongerCount)
                .trustScore(trustScore)
                .trustLabel(trustLabel)
                .createdAt(createdAt)
                .expiresAt(expiresAt)
                .build();
    }
}
