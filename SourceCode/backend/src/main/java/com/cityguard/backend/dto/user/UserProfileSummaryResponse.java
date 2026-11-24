package com.cityguard.backend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO thông tin cá nhân và thống kê báo cáo (theo SRS Trang 9 FR-PROF-01).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileSummaryResponse {
    private Long id;
    private String username;
    private String email;
    private String displayName;
    private String avatarUrl;
    private String role;
    private long reportCount;
    private long confirmedReports;
}

