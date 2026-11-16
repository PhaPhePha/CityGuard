package com.cityguard.backend.controller;

import com.cityguard.backend.dto.response.ApiResponse;
import com.cityguard.backend.dto.response.ReportResponse;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.enums.ReportType;
import com.cityguard.backend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller quản lý và truy vấn báo cáo sự cố giao thông (theo DacTaKyThuat Trang 8, Mục 6.0).
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Report Management", description = "Các API truy vấn và quản lý báo cáo sự cố giao thông")
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    @Operation(
            summary = "Lấy danh sách báo cáo sự cố",
            description = "Lấy danh sách các báo cáo sự cố giao thông theo vùng bản đồ (bounding box), loại sự cố và trạng thái"
    )
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReports(
            @Parameter(description = "Tọa độ bounding box dạng minLng,minLat,maxLng,maxLat (ví dụ: 106.70,10.79,106.72,10.81)")
            @RequestParam(required = false) String bbox,

            @Parameter(description = "Lọc theo loại sự cố")
            @RequestParam(required = false) ReportType type,

            @Parameter(description = "Lọc theo trạng thái báo cáo")
            @RequestParam(required = false) ReportStatus status
    ) {
        List<ReportResponse> reports = reportService.getReports(bbox, type, status);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Lấy chi tiết báo cáo sự cố theo ID",
            description = "Trả về thông tin chi tiết của một báo cáo sự cố cụ thể kèm điểm tin cậy và số lượng vote"
    )
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(
            @Parameter(description = "Mã định danh báo cáo sự cố (ví dụ: rpt_001)")
            @PathVariable String id
    ) {
        ReportResponse report = reportService.getReportById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin báo cáo thành công", report));
    }
}
