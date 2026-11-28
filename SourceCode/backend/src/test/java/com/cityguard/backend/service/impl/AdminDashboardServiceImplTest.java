package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.admin.dashboard.AdminDashboardSummaryResponse;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.repository.ReportRepository;
import com.cityguard.backend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportRepository reportRepository;

    @InjectMocks
    private AdminDashboardServiceImpl adminDashboardService;

    @Test
    @DisplayName("1. getSummary() đếm chính xác totalUsers, totalReports, activeReports (ACTIVE) và pendingReports (PENDING_VERIFY)")
    void getSummary_shouldAggregateCountsByStatusCorrectly() {
        when(userRepository.count()).thenReturn(5L);
        when(reportRepository.count()).thenReturn(6L);
        when(reportRepository.countByStatus(ReportStatus.ACTIVE)).thenReturn(2L);
        when(reportRepository.countByStatus(ReportStatus.PENDING_VERIFY)).thenReturn(2L);

        AdminDashboardSummaryResponse summary = adminDashboardService.getSummary();

        assertThat(summary).isNotNull();
        assertThat(summary.getTotalUsers()).isEqualTo(5L);
        assertThat(summary.getTotalReports()).isEqualTo(6L);
        assertThat(summary.getActiveReports()).isEqualTo(2L);
        assertThat(summary.getPendingReports()).isEqualTo(2L);

        verify(userRepository).count();
        verify(reportRepository).count();
        verify(reportRepository).countByStatus(ReportStatus.ACTIVE);
        verify(reportRepository).countByStatus(ReportStatus.PENDING_VERIFY);
        verifyNoMoreInteractions(userRepository, reportRepository);
    }

    @Test
    @DisplayName("2. getSummary() trả về 0 cho tất cả chỉ số khi hệ thống chưa có dữ liệu")
    void getSummary_whenDatabaseEmpty_shouldReturnZeros() {
        when(userRepository.count()).thenReturn(0L);
        when(reportRepository.count()).thenReturn(0L);
        when(reportRepository.countByStatus(ReportStatus.ACTIVE)).thenReturn(0L);
        when(reportRepository.countByStatus(ReportStatus.PENDING_VERIFY)).thenReturn(0L);

        AdminDashboardSummaryResponse summary = adminDashboardService.getSummary();

        assertThat(summary.getTotalUsers()).isZero();
        assertThat(summary.getTotalReports()).isZero();
        assertThat(summary.getActiveReports()).isZero();
        assertThat(summary.getPendingReports()).isZero();
    }
}
