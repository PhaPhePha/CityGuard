package com.cityguard.backend.dto.admin.report;

import com.cityguard.backend.enums.ReportStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO yêu cầu cập nhật trạng thái báo cáo sự cố (PATCH /api/v1/admin/reports/{publicId}/status).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateReportStatusRequest {

    @NotNull(message = "Trạng thái báo cáo không được để trống")
    private ReportStatus status;

    @NotBlank(message = "Lý do kiểm duyệt không được để trống")
    @Size(max = 1000, message = "Lý do kiểm duyệt không được vượt quá 1000 ký tự")
    private String reason;
}
