package com.cityguard.backend.dto.request;

import com.cityguard.backend.enums.ReportType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

public record CreateReportRequest(
        @NotBlank String requestId,
        @NotNull ReportType type,
        @NotBlank String description,
        @NotBlank String addressSnapshot,
        @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
        @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude,
        String imageUrl
) {}
