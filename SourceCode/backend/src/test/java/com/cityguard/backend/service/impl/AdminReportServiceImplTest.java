package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.admin.report.AdminUpdateReportStatusRequest;
import com.cityguard.backend.entity.Report;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.repository.AdminLogRepository;
import com.cityguard.backend.repository.ReportRepository;
import com.cityguard.backend.security.CustomUserDetails;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminReportServiceImplTest {

    @ParameterizedTest
    @CsvSource({
            "ACTIVE, HIDDEN, HIDE_REPORT",
            "HIDDEN, ACTIVE, RESTORE_REPORT",
            "ACTIVE, RESOLVED, UPDATE_REPORT_STATUS"
    })
    void updateReportStatus_usesRequestedStatusAndRecordsAuditLog(
            ReportStatus current, ReportStatus target, String expectedAction
    ) {
        var reportRepository = mock(ReportRepository.class);
        var adminLogRepository = mock(AdminLogRepository.class);
        var service = new AdminReportServiceImpl(reportRepository, adminLogRepository);
        var report = Report.builder().id(10L).publicId("rpt_test").status(current).build();
        var request = AdminUpdateReportStatusRequest.builder().status(target).reason("Kiểm duyệt").build();
        var actor = User.builder().id(20L).role("MODERATOR").status("ACTIVE").build();
        var principal = new CustomUserDetails(actor);
        when(reportRepository.findByPublicIdForUpdate(report.getPublicId())).thenReturn(Optional.of(report));

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        try {
            var response = service.updateReportStatus(report.getPublicId(), request);

            assertThat(report.getStatus()).isEqualTo(target);
            assertThat(response.getStatus()).isEqualTo(target);
            verify(adminLogRepository).save(argThat(log ->
                    actor.equals(log.getActor()) && report.getId().equals(log.getTargetId())
                            && "REPORT".equals(log.getTargetType()) && request.getReason().equals(log.getReason())
                            && expectedAction.equals(log.getAction())));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
