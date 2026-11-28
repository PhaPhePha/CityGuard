package com.cityguard.backend.admin;

import com.cityguard.backend.controller.AdminReportController;
import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.dto.admin.report.AdminReportDetailResponse;
import com.cityguard.backend.dto.admin.report.AdminReportSummaryResponse;
import com.cityguard.backend.dto.admin.report.AdminUpdateReportStatusRequest;
import com.cityguard.backend.entity.Category;
import com.cityguard.backend.entity.Report;
import com.cityguard.backend.entity.ReportMetrics;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.exception.GlobalExceptionHandler;
import com.cityguard.backend.security.CustomUserDetails;
import com.cityguard.backend.security.CustomUserDetailsService;
import com.cityguard.backend.security.JwtAuthenticationFilter;
import com.cityguard.backend.security.JwtService;
import com.cityguard.backend.security.RestAccessDeniedHandler;
import com.cityguard.backend.security.RestAuthenticationEntryPoint;
import com.cityguard.backend.security.SecurityConfig;
import com.cityguard.backend.service.AdminReportService;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bộ test chuẩn bị cho Phase 3 (Report Management):
 * - Kiểm tra AdminReportSummaryResponse & AdminReportDetailResponse không làm lộ reports.id BIGINT,
 *   giữ nguyên trustScore = null / trustLabel = null khi chưa có đánh giá.
 * - Kiểm tra AdminPageResponse trả đúng các trường items, page, size, totalItems, totalPages, isLast.
 * - Kiểm tra validation cho AdminUpdateReportStatusRequest (bắt buộc status và reason không rỗng).
 * - Kiểm tra phân quyền ADMIN / MODERATOR / USER / 401 cho 3 endpoints của Phase 3.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AdminPhase3ReportContractAndSecurityTest.Phase3TestConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=CityGuardTestSecretKeyWithAtLeast32BytesLengthForHS256!",
        "jwt.access-expiration-ms=3600000",
        "jwt.refresh-expiration-ms=604800000"
})
class AdminPhase3ReportContractAndSecurityTest {

    private static final String PUBLIC_ID = "rpt_01M6P4YPXFBG21T29EZJ31RR0Q";
    private static final String STATUS_PATH = "/api/v1/admin/reports/" + PUBLIC_ID + "/status";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    @Qualifier("springSecurityFilterChain")
    private Filter springSecurityFilterChain;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private AdminReportService adminReportService;

    private MockMvc mockMvc;

    private User adminUser;
    private User moderatorUser;
    private User citizenUser;

    @BeforeEach
    void setUp() {
        reset(userDetailsService, adminReportService);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(springSecurityFilterChain)
                .build();

        adminUser = User.builder()
                .id(4L)
                .username("demo_admin")
                .email("admin@example.invalid")
                .passwordHash("$2a$10$hash")
                .displayName("Demo Admin")
                .role("ADMIN")
                .status("ACTIVE")
                .build();

        moderatorUser = User.builder()
                .id(5L)
                .username("demo_moderator")
                .email("mod@example.invalid")
                .passwordHash("$2a$10$hash")
                .displayName("Demo Moderator")
                .role("MODERATOR")
                .status("ACTIVE")
                .build();

        citizenUser = User.builder()
                .id(1L)
                .username("demo_citizen")
                .email("citizen@example.invalid")
                .passwordHash("$2a$10$hash")
                .displayName("Demo Citizen")
                .role("USER")
                .status("ACTIVE")
                .build();

        when(userDetailsService.loadUserByUsername("demo_admin")).thenReturn(new CustomUserDetails(adminUser));
        when(userDetailsService.loadUserByUsername("demo_moderator")).thenReturn(new CustomUserDetails(moderatorUser));
        when(userDetailsService.loadUserByUsername("demo_citizen")).thenReturn(new CustomUserDetails(citizenUser));
    }

    @Test
    @DisplayName("1. AdminReportSummaryResponse & AdminReportDetailResponse dùng publicId (rpt_...), bảo toàn trustScore=null và thông tin reporter")
    void reportDtos_fromEntity_shouldUsePublicIdAndPreserveNullTrustScore() {
        Category category = Category.builder()
                .id(4L)
                .code("FLOOD")
                .displayName("Ngập nước")
                .color("#3B82F6")
                .build();

        ReportMetrics metricsWithNullTrust = ReportMetrics.builder()
                .reportId(999L)
                .confirmCount(0L)
                .noLongerCount(0L)
                .trustScore(null)
                .trustLabel(null)
                .build();

        Report report = Report.builder()
                .id(999L) // Khóa chính nội bộ BIGINT không được lộ thành report id
                .publicId("rpt_01M6P63ASFNX3Z0R49VXX9BKG6")
                .user(citizenUser)
                .category(category)
                .description("Nước ngập sâu nửa bánh xe")
                .latitude(10.7324)
                .longitude(106.7210)
                .addressSnapshot("Quận 7, TP.HCM")
                .status(ReportStatus.PENDING_VERIFY)
                .reportMetrics(metricsWithNullTrust)
                .createdAt(OffsetDateTime.of(2026, 11, 20, 9, 30, 0, 0, ZoneOffset.UTC))
                .expiresAt(OffsetDateTime.of(2026, 11, 20, 15, 30, 0, 0, ZoneOffset.UTC))
                .build();

        AdminReportSummaryResponse summary = AdminReportSummaryResponse.from(report);
        assertThat(summary.getPublicId()).isEqualTo("rpt_01M6P63ASFNX3Z0R49VXX9BKG6");
        assertThat(summary.getCategoryId()).isEqualTo(4L);
        assertThat(summary.getCategoryCode()).isEqualTo("FLOOD");
        assertThat(summary.getCategoryDisplayName()).isEqualTo("Ngập nước");
        assertThat(summary.getCategoryColor()).isEqualTo("#3B82F6");
        assertThat(summary.getTrustScore()).isNull();
        assertThat(summary.getTrustLabel()).isNull();

        AdminReportDetailResponse detail = AdminReportDetailResponse.from(report);
        assertThat(detail.getPublicId()).isEqualTo("rpt_01M6P63ASFNX3Z0R49VXX9BKG6");
        assertThat(detail.getReporterId()).isEqualTo(1L);
        assertThat(detail.getReporterUsername()).isEqualTo("demo_citizen");
        assertThat(detail.getReporterDisplayName()).isEqualTo("Demo Citizen");
    }

    @Test
    @DisplayName("2. GET /api/v1/admin/reports trả đúng cấu trúc phân trang React (items, page, size, totalItems, totalPages, isLast)")
    void getReports_withAdminOrModerator_shouldReturnAdminPageResponseContract() throws Exception {
        String modToken = jwtService.generateAccessToken(moderatorUser);
        var item = AdminReportSummaryResponse.builder().publicId(PUBLIC_ID).build();
        var page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);
        when(adminReportService.getReports(null, null, null, 0, 10, "createdAt", "DESC"))
                .thenReturn(AdminPageResponse.of(page));

        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + modToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].publicId").value("rpt_01M6P4YPXFBG21T29EZJ31RR0Q"))
                .andExpect(jsonPath("$.data.items[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(10))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.isLast").value(true));
    }

    @Test
    @DisplayName("3. PATCH /api/v1/admin/reports/{publicId}/status kiểm tra validation @NotNull status và @NotBlank reason")
    void updateReportStatus_withBlankReason_shouldReturn400BadRequest() throws Exception {
        String adminToken = jwtService.generateAccessToken(adminUser);

        mockMvc.perform(patch("/api/v1/admin/reports/rpt_01M6P4YPXFBG21T29EZJ31RR0Q/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\",\"reason\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        verifyNoInteractions(adminReportService);
    }

    @Test
    void updateReportStatus_withMissingStatus_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(patch(STATUS_PATH)
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken(adminUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Spam\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
        verifyNoInteractions(adminReportService);
    }

    @Test
    void updateReportStatus_withModerator_shouldReturnUpdatedDetail() throws Exception {
        var request = AdminUpdateReportStatusRequest.builder().status(ReportStatus.HIDDEN).reason("Spam").build();
        var detail = AdminReportDetailResponse.builder().publicId(PUBLIC_ID).status(ReportStatus.HIDDEN).build();
        when(adminReportService.updateReportStatus(PUBLIC_ID, request)).thenReturn(detail);

        mockMvc.perform(patch(STATUS_PATH)
                        .header("Authorization", "Bearer " + jwtService.generateAccessToken(moderatorUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\",\"reason\":\"Spam\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.publicId").value(PUBLIC_ID))
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));
    }

    @Test
    @DisplayName("4. Phân quyền Phase 3: ROLE_USER bị chặn 403, không JWT bị chặn 401 trên cả 3 endpoints")
    void reportEndpoints_withRoleUserOrNoJwt_shouldBeDenied() throws Exception {
        String userToken = jwtService.generateAccessToken(citizenUser);

        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/reports/rpt_01M6P4YPXFBG21T29EZJ31RR0Q")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/v1/admin/reports/rpt_01M6P4YPXFBG21T29EZJ31RR0Q/status")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\",\"reason\":\"Spam\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/reports"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(patch(STATUS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"HIDDEN\",\"reason\":\"Spam\"}"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(adminReportService);
    }

    @Configuration
    @EnableWebMvc
    @Import({
            SecurityConfig.class,
            JwtAuthenticationFilter.class,
            JwtService.class,
            RestAuthenticationEntryPoint.class,
            RestAccessDeniedHandler.class,
            GlobalExceptionHandler.class,
            AdminReportController.class
    })
    static class Phase3TestConfig {

        @Bean
        public JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        public CustomUserDetailsService customUserDetailsService() {
            return Mockito.mock(CustomUserDetailsService.class);
        }
        @Bean
        public AdminReportService adminReportService() {
            return Mockito.mock(AdminReportService.class);
        }
    }
}
