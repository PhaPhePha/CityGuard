package com.cityguard.backend.dto.response;

import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.enums.ReportType;
import com.cityguard.backend.enums.TrustLabel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dữ liệu báo cáo sự cố trả về client (theo DacTaKyThuat Trang 8 Mục 6.1 và Trang 9 Mục 6.2).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private String id;
    private ReportType type;
    private ReportStatus status;
    private Integer trustScore;
    private TrustLabel trustLabel;
    private Integer confirmCount;
    private Integer noLongerCount;
    private String userVote;
    private Double latitude;
    private Double longitude;
    private String addressSnapshot;
    private String description;
    private String imageUrl;
    private String createdAt;
    private String expiresAt;
}

