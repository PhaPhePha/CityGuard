package com.cityguard.backend.dto.admin.user;

import com.cityguard.backend.dto.admin.report.AdminReportSummaryResponse;
import com.cityguard.backend.entity.User;
import lombok.Getter;

import java.util.List;

@Getter
public class AdminUserDetailResponse extends AdminUserListItemResponse {

    private final long totalReports;
    private final List<AdminReportSummaryResponse> recentReports;

    private AdminUserDetailResponse(
            AdminUserListItemResponse user,
            long totalReports,
            List<AdminReportSummaryResponse> recentReports
    ){
        super(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
        this.totalReports = totalReports;
        this.recentReports = recentReports;
    }

    public static AdminUserDetailResponse from(
            User user,
            long totalReports,
            List<AdminReportSummaryResponse> recentReports
    ){
        return new AdminUserDetailResponse(
                AdminUserListItemResponse.from(user),
                totalReports,
                recentReports
        );
    }

}
