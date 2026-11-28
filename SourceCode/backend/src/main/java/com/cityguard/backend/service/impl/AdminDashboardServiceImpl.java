package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.admin.dashboard.AdminDashboardSummaryResponse;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.repository.ReportRepository;
import com.cityguard.backend.repository.UserRepository;
import com.cityguard.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardSummaryResponse getSummary(){
        long totalUsers = userRepository.count();
        long totalReports = reportRepository.count();

        long activeReports = reportRepository.countByStatus(ReportStatus.ACTIVE);

        long pendingReports = reportRepository.countByStatus(ReportStatus.PENDING_VERIFY);

        return AdminDashboardSummaryResponse.builder()
                .totalUsers(totalUsers)
                .totalReports(totalReports)
                .activeReports(activeReports)
                .pendingReports(pendingReports)
                .build();
    }
}
