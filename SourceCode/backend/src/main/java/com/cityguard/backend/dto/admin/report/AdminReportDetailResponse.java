package com.cityguard.backend.dto.admin.report;

import com.cityguard.backend.entity.Category;
import com.cityguard.backend.entity.Report;
import com.cityguard.backend.entity.ReportMetrics;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

/**
 * DTO chi tiết báo cáo sự cố cho Admin (GET & PATCH /api/v1/admin/reports/{publicId}).
 * Bao gồm các trường của AdminReportSummaryResponse và thông tin người tạo báo cáo (reporter).
 * Tuyệt đối không làm lộ reports.id (BIGINT).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportDetailResponse {

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
    private Long reporterId;
    private String reporterUsername;
    private String reporterDisplayName;
    private String reporterAvatarUrl;

    public static AdminReportDetailResponse from(Report report) {
        if (report == null) {
            return null;
        }
        Category category = report.getCategory();
        ReportMetrics metrics = report.getReportMetrics();
        User reporter = report.getUser();

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

        return AdminReportDetailResponse.builder()
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
                .reporterId(reporter != null ? reporter.getId() : null)
                .reporterUsername(reporter != null ? reporter.getUsername() : null)
                .reporterDisplayName(reporter != null ? reporter.getDisplayName() : null)
                .reporterAvatarUrl(reporter != null ? reporter.getAvatarUrl() : null)
                .build();
    }
}
