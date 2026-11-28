package com.cityguard.backend.dto.report;

import com.cityguard.backend.enums.ReportType;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * DTO yêu cầu tạo báo cáo sự cố mới (khớp Schema V3 & chuẩn hóa theo NFR-MAINT-01).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequest {

    @NotNull(message = "Loại sự cố không được để trống")
    @JsonAlias({"categoryCode", "category_code"})
    private ReportType type;

    @Size(max = 2000, message = "Mô tả không được vượt quá 2000 ký tự")
    private String description;

    @NotNull(message = "Vĩ độ (latitude) không được để trống")
    @DecimalMin(value = "-90.0", message = "Vĩ độ tối thiểu là -90")
    @DecimalMax(value = "90.0", message = "Vĩ độ tối đa là 90")
    private Double latitude;

    @NotNull(message = "Kinh độ (longitude) không được để trống")
    @DecimalMin(value = "-180.0", message = "Kinh độ tối thiểu là -180")
    @DecimalMax(value = "180.0", message = "Kinh độ tối đa là 180")
    private Double longitude;

    @Size(max = 500, message = "Địa chỉ snapshot không được vượt quá 500 ký tự")
    private String addressSnapshot;

    @Pattern(
            regexp = "^(https://\\S+|http://10\\.0\\.2\\.2:8080/uploads/\\S+)$",
            message = "URL hình ảnh không hợp lệ"
    )
    private String imageUrl;
    private UUID clientRequestId;
}
