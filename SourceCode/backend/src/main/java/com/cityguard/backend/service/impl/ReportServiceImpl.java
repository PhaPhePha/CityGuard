package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.response.ReportResponse;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.enums.ReportType;
import com.cityguard.backend.enums.TrustLabel;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.service.ReportService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Triển khai ReportService với dữ liệu mẫu phục vụ Sprint 1 mock API.
 */
@Service
public class ReportServiceImpl implements ReportService {

    private final List<ReportResponse> sampleReports;

    public ReportServiceImpl() {
        List<ReportResponse> list = new ArrayList<>();

        // Mẫu 1: Ngã tư Hàng Xanh
        list.add(ReportResponse.builder()
                .id("rpt_001")
                .type(ReportType.TRAFFIC_JAM)
                .status(ReportStatus.ACTIVE)
                .trustScore(85)
                .trustLabel(TrustLabel.HIGH)
                .confirmCount(9)
                .noLongerCount(1)
                .userVote("CONFIRMED")
                .latitude(10.8012)
                .longitude(106.7145)
                .addressSnapshot("Ngã tư Hàng Xanh, Bình Thạnh, TP.HCM")
                .description("Kẹt xe kéo dài từ Ngã tư Hàng Xanh đến cầu Điện Biên Phủ")
                .imageUrl("https://images.unsplash.com/photo-1544620347-c4fd4a3d5957")
                .createdAt("2026-10-17T09:20:00Z")
                .expiresAt("2026-10-17T11:20:00Z")
                .build());

        // Mẫu 2: Ung Văn Khiêm
        list.add(ReportResponse.builder()
                .id("rpt_002")
                .type(ReportType.FLOOD)
                .status(ReportStatus.ACTIVE)
                .trustScore(60)
                .trustLabel(TrustLabel.MEDIUM)
                .confirmCount(4)
                .noLongerCount(1)
                .userVote(null)
                .latitude(10.8045)
                .longitude(106.7180)
                .addressSnapshot("Đường Ung Văn Khiêm, Bình Thạnh, TP.HCM")
                .description("Ngập nước sâu đoạn qua trường Đại học Hutech")
                .imageUrl("https://images.unsplash.com/photo-1515694346937-94d85e41e6f0")
                .createdAt("2026-10-17T09:00:00Z")
                .expiresAt("2026-10-17T21:00:00Z")
                .build());

        // Mẫu 3: Điện Biên Phủ
        list.add(ReportResponse.builder()
                .id("rpt_003")
                .type(ReportType.FALLEN_TREE)
                .status(ReportStatus.ACTIVE)
                .trustScore(20)
                .trustLabel(TrustLabel.PENDING_VERIFY)
                .confirmCount(1)
                .noLongerCount(0)
                .userVote(null)
                .latitude(10.7980)
                .longitude(106.7050)
                .addressSnapshot("Đường Điện Biên Phủ, Bình Thạnh, TP.HCM")
                .description("Cây xanh gãy nhánh chắn nửa làn xe máy")
                .imageUrl("https://images.unsplash.com/photo-1584467735871-8e85353a8413")
                .createdAt("2026-10-17T09:30:00Z")
                .expiresAt("2026-10-17T21:30:00Z")
                .build());

        this.sampleReports = Collections.unmodifiableList(list);
    }

    @Override
    public List<ReportResponse> getReports(String bbox, ReportType type, ReportStatus status) {
        return sampleReports.stream()
                .filter(report -> type == null || report.getType() == type)
                .filter(report -> status == null || report.getStatus() == status)
                .toList();
    }

    @Override
    public ReportResponse getReportById(String id) {
        return sampleReports.stream()
                .filter(report -> report.getId().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
    }
}
