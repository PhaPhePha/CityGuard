package com.cityguard.backend.controller;

import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.dto.common.PageResponse;
import com.cityguard.backend.dto.report.ReportResponse;
import com.cityguard.backend.dto.user.UserProfileSummaryResponse;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản lý hồ sơ cá nhân và báo cáo của tôi (theo DacTaKyThuat Trang 8 Mục 6.0 & SRS Trang 9 FR-PROF-01).
 */
@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
@Tag(name = "Profile & My Reports", description = "Các API thông tin cá nhân và báo cáo do người dùng tạo")
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping("/reports")
    @Operation(
            summary = "Danh sách báo cáo của tôi (phân trang)",
            description = "Lấy danh sách các báo cáo do tài khoản hiện tại tạo, hỗ trợ lọc theo trạng thái và phân trang (trường items)"
    )
    public ResponseEntity<ApiResponse<PageResponse<ReportResponse>>> getMyReports(
            @Parameter(description = "Lọc theo trạng thái báo cáo (ACTIVE, PENDING_VERIFY, EXPIRED...)")
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<ReportResponse> response = profileService.getMyReports(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/summary")
    @Operation(
            summary = "Thống kê hồ sơ cá nhân",
            description = "Lấy thông tin tài khoản kèm số lượng báo cáo đã tạo và số báo cáo đã xác nhận"
    )
    public ResponseEntity<ApiResponse<UserProfileSummaryResponse>> getProfileSummary() {
        UserProfileSummaryResponse response = profileService.getProfileSummary();
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin hồ sơ thành công", response));
    }
}
