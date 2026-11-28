package com.cityguard.backend.controller;

import com.cityguard.backend.dto.comment.CommentRequest;
import com.cityguard.backend.dto.comment.CommentResponse;
import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.dto.common.PageResponse;
import com.cityguard.backend.dto.report.CreateReportRequest;
import com.cityguard.backend.dto.report.ReportResponse;
import com.cityguard.backend.dto.report.VoteRequest;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.enums.ReportType;
import com.cityguard.backend.service.CommentService;
import com.cityguard.backend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controller quản lý và truy vấn báo cáo sự cố giao thông (theo DacTaKyThuat Trang 8, Mục 6.0 & WBS 4.3.1).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Report Management", description = "Các API truy vấn và quản lý báo cáo sự cố giao thông")
public class ReportController {

    private final ReportService reportService;
    private final CommentService commentService;

    @GetMapping("/api/v1/reports")
    @Operation(
            summary = "Lấy danh sách báo cáo sự cố",
            description = "Lấy danh sách các báo cáo sự cố giao thông theo vùng bản đồ (bounding box), loại sự cố, trạng thái và sắp xếp (nearby/latest)"
    )
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReports(
            @Parameter(description = "Tọa độ bounding box dạng minLng,minLat,maxLng,maxLat (ví dụ: 106.70,10.79,106.72,10.81)")
            @RequestParam(required = false) String bbox,

            @Parameter(description = "Lọc theo loại sự cố")
            @RequestParam(required = false) ReportType type,

            @Parameter(description = "Lọc theo trạng thái báo cáo")
            @RequestParam(required = false) ReportStatus status,

            @Parameter(description = "Chế độ sắp xếp: nearby (theo khoảng cách) hoặc latest (mới nhất)")
            @RequestParam(required = false) String sort,

            @Parameter(description = "Vĩ độ hiện tại của người dùng (dùng khi sort=nearby)")
            @RequestParam(required = false) Double userLat,

            @Parameter(description = "Kinh độ hiện tại của người dùng (dùng khi sort=nearby)")
            @RequestParam(required = false) Double userLng
    ) {
        List<ReportResponse> reports = reportService.getReports(bbox, type, status, sort, userLat, userLng);
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/api/v1/reports/mine")
    @Operation(
            summary = "Danh sách báo cáo của tôi (mảng phẳng)",
            description = "Lấy danh sách các báo cáo do tài khoản hiện tại tạo dưới dạng danh sách phẳng"
    )
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getMyReports() {
        List<ReportResponse> reports = reportService.getMyReports();
        return ResponseEntity.ok(ApiResponse.success(reports));
    }

    @GetMapping("/api/v1/reports/{id}")
    @Operation(
            summary = "Lấy chi tiết báo cáo sự cố theo ID",
            description = "Trả về thông tin chi tiết của một báo cáo sự cố cụ thể kèm điểm tin cậy, số lượng vote và danh sách bình luận"
    )
    public ResponseEntity<ApiResponse<ReportResponse>> getReportById(
            @Parameter(description = "Mã định danh báo cáo sự cố (ví dụ: rpt_001 hoặc public_id)")
            @PathVariable String id
    ) {
        ReportResponse report = reportService.getReportById(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy thông tin báo cáo thành công", report));
    }

    @PostMapping("/api/v1/reports")
    @Operation(
            summary = "Tạo báo cáo sự cố mới",
            description = "Người dùng đã đăng nhập gửi thông tin báo cáo sự cố (có idempotency qua clientRequestId)"
    )
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
            @Valid @RequestBody CreateReportRequest request
    ) {
        ReportResponse response = reportService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo báo cáo thành công", response));
    }

    @DeleteMapping("/api/v1/reports/{id}")
    @Operation(summary = "Xóa báo cáo của tôi", description = "Chỉ người tạo báo cáo mới được phép xóa báo cáo")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable String id) {
        reportService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Xóa báo cáo thành công", null));
    }

    @RequestMapping(value = "/api/v1/reports/{id}/vote", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(
            summary = "Bình chọn / xác nhận sự cố (hỗ trợ PUT & POST, toggle hủy vote)",
            description = "Xác nhận sự cố còn tồn tại (CONFIRMED) hoặc không còn nữa (NO_LONGER), bấm lại cùng loại sẽ hủy vote"
    )
    public ResponseEntity<ApiResponse<ReportResponse>> vote(
            @Parameter(description = "Mã định danh báo cáo sự cố")
            @PathVariable String id,
            @Valid @RequestBody VoteRequest request
    ) {
        ReportResponse response = reportService.vote(id, request);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật xác nhận sự cố thành công", response));
    }

    @GetMapping("/api/v1/reports/{id}/comments")
    @Operation(
            summary = "Lấy danh sách bình luận của báo cáo",
            description = "Lấy danh sách bình luận được phân trang (mặc định 20, tối đa 50)"
    )
    public ResponseEntity<ApiResponse<PageResponse<CommentResponse>>> getComments(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<CommentResponse> response = commentService.getComments(id, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/api/v1/reports/{id}/comments")
    @Operation(
            summary = "Thêm bình luận cho báo cáo",
            description = "Người dùng đã đăng nhập thêm bình luận vào báo cáo sự cố"
    )
    public ResponseEntity<ApiResponse<CommentResponse>> addComment(
            @PathVariable String id,
            @Valid @RequestBody CommentRequest request
    ) {
        CommentResponse response = commentService.addComment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm bình luận thành công", response));
    }
}
