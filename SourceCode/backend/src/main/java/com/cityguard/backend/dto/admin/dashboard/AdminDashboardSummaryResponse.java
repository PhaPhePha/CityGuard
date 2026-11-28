package com.cityguard.backend.dto.admin.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi thống kê tổng quan cho Admin Dashboard
 * (theo ADMIN_API_CONTRACT.md mục 3.2 & Admin_web/src/types/dashboard.types.ts).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardSummaryResponse {

    private long totalUsers;
    private long totalReports;
    private long activeReports;
    private long pendingReports;
}
