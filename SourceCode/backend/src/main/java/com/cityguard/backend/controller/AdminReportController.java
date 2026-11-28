package com.cityguard.backend.controller;

import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.dto.admin.report.AdminReportDetailResponse;
import com.cityguard.backend.dto.admin.report.AdminReportSummaryResponse;
import com.cityguard.backend.dto.admin.report.AdminUpdateReportStatusRequest;
import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.service.AdminReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping("/{publicId}")
    public ResponseEntity<ApiResponse<AdminReportDetailResponse>>getReportDetail(@PathVariable String publicId){
        AdminReportDetailResponse response = adminReportService.getReportDetail(publicId);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminPageResponse<AdminReportSummaryResponse>>>getReports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false)ReportStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String direction
            ){
        var response = adminReportService.getReports(
                keyword, categoryId, status,page,size,sortBy,direction
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{publicId}/status")
    public ResponseEntity<ApiResponse<AdminReportDetailResponse>> updateReportStatus(
            @PathVariable String publicId,
            @Valid @RequestBody AdminUpdateReportStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(adminReportService.updateReportStatus(publicId, request)));
    }
}
