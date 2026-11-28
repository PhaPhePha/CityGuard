package com.cityguard.backend.admin;

import com.cityguard.backend.controller.AdminCategoryController;
import com.cityguard.backend.controller.AdminDashboardController;
import com.cityguard.backend.dto.admin.category.AdminCategoryResponse;
import com.cityguard.backend.entity.Category;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ReportStatus;
import com.cityguard.backend.exception.GlobalExceptionHandler;
import com.cityguard.backend.repository.CategoryRepository;
import com.cityguard.backend.repository.ReportRepository;
import com.cityguard.backend.repository.UserRepository;
import com.cityguard.backend.security.CustomUserDetails;
import com.cityguard.backend.security.CustomUserDetailsService;
import com.cityguard.backend.security.JwtAuthenticationFilter;
import com.cityguard.backend.security.JwtService;
import com.cityguard.backend.security.RestAccessDeniedHandler;
import com.cityguard.backend.security.RestAuthenticationEntryPoint;
import com.cityguard.backend.security.SecurityConfig;
import com.cityguard.backend.service.impl.AdminDashboardServiceImpl;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Kiểm thử tích hợp MockMvc cho Phase 2 (Dashboard Summary & Categories)
 * sử dụng AdminDashboardController, AdminDashboardServiceImpl và AdminCategoryController thật.
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AdminPhase2ContractAndSecurityTest.Phase2RealControllerTestConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=CityGuardTestSecretKeyWithAtLeast32BytesLengthForHS256!",
        "jwt.access-expiration-ms=3600000",
        "jwt.refresh-expiration-ms=604800000"
})
class AdminPhase2ContractAndSecurityTest {

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
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private MockMvc mockMvc;

    private User adminUser;
    private User moderatorUser;
    private User citizenUser;

    @BeforeEach
    void setUp() {
        reset(userDetailsService, userRepository, reportRepository, categoryRepository);
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
    @DisplayName("1. AdminCategoryResponse.from(Category) ánh xạ đúng displayName -> name và giữ nguyên id, code, color")
    void adminCategoryResponse_fromEntity_shouldMapDisplayNameToName() {
        Category entity = Category.builder()
                .id(1L)
                .code("TRAFFIC_JAM")
                .displayName("Kẹt xe")
                .icon("ic_traffic_jam")
                .color("#EF4444")
                .defaultTtlSeconds(14400)
                .isActive(true)
                .build();

        AdminCategoryResponse dto = AdminCategoryResponse.from(entity);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getCode()).isEqualTo("TRAFFIC_JAM");
        assertThat(dto.getName()).isEqualTo("Kẹt xe");
        assertThat(dto.getColor()).isEqualTo("#EF4444");
        assertThat(AdminCategoryResponse.from(null)).isNull();
    }

    @Test
    @DisplayName("2. AdminDashboardController + AdminDashboardServiceImpl thật trả đúng 4 trường JSON contract cho ADMIN và MODERATOR")
    void dashboardSummary_realControllerAndService_shouldReturnContractFields() throws Exception {
        when(userRepository.count()).thenReturn(120L);
        when(reportRepository.count()).thenReturn(450L);
        when(reportRepository.countByStatus(ReportStatus.ACTIVE)).thenReturn(85L);
        when(reportRepository.countByStatus(ReportStatus.PENDING_VERIFY)).thenReturn(24L);

        String adminToken = jwtService.generateAccessToken(adminUser);
        String modToken = jwtService.generateAccessToken(moderatorUser);

        mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").value(120))
                .andExpect(jsonPath("$.data.totalReports").value(450))
                .andExpect(jsonPath("$.data.activeReports").value(85))
                .andExpect(jsonPath("$.data.pendingReports").value(24));

        mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + modToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalUsers").value(120))
                .andExpect(jsonPath("$.data.activeReports").value(85))
                .andExpect(jsonPath("$.data.pendingReports").value(24));

        verify(reportRepository, Mockito.times(2)).countByStatus(ReportStatus.ACTIVE);
        verify(reportRepository, Mockito.times(2)).countByStatus(ReportStatus.PENDING_VERIFY);
    }

    @Test
    @DisplayName("3. AdminCategoryController thật gọi CategoryRepository(Sort ASC id) và trả đúng 6 danh mục V3 cho ADMIN và MODERATOR")
    void categories_realController_shouldReturnV3CategoriesContract() throws Exception {
        List<Category> v3Categories = List.of(
                Category.builder().id(1L).code("TRAFFIC_JAM").displayName("Kẹt xe").color("#EF4444").build(),
                Category.builder().id(2L).code("ROAD_BLOCK").displayName("Rào chắn / Cấm đường").color("#F97316").build(),
                Category.builder().id(3L).code("ROAD_DAMAGE").displayName("Đường hư hỏng / Ổ gà").color("#F59E0B").build(),
                Category.builder().id(4L).code("FLOOD").displayName("Ngập nước").color("#3B82F6").build(),
                Category.builder().id(5L).code("BROKEN_TRAFFIC_LIGHT").displayName("Đèn tín hiệu hỏng").color("#8B5CF6").build(),
                Category.builder().id(6L).code("FALLEN_TREE").displayName("Cây đổ").color("#10B981").build()
        );
        when(categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))).thenReturn(v3Categories);

        String modToken = jwtService.generateAccessToken(moderatorUser);

        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + modToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(6))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].code").value("TRAFFIC_JAM"))
                .andExpect(jsonPath("$.data[0].name").value("Kẹt xe"))
                .andExpect(jsonPath("$.data[0].color").value("#EF4444"))
                .andExpect(jsonPath("$.data[5].id").value(6))
                .andExpect(jsonPath("$.data[5].code").value("FALLEN_TREE"))
                .andExpect(jsonPath("$.data[5].name").value("Cây đổ"))
                .andExpect(jsonPath("$.data[5].color").value("#10B981"));

        verify(categoryRepository).findAll(Sort.by(Sort.Direction.ASC, "id"));
    }

    @Test
    @DisplayName("4. USER và request không có JWT bị chặn (403 / 401) trước khi vào Controller/Repository")
    void dashboardAndCategories_withUserOrNoJwt_shouldBeDenied() throws Exception {
        String userToken = jwtService.generateAccessToken(citizenUser);

        mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        verifyNoInteractions(userRepository, reportRepository, categoryRepository);
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
            AdminDashboardController.class,
            AdminDashboardServiceImpl.class,
            AdminCategoryController.class
    })
    static class Phase2RealControllerTestConfig {

        @Bean
        public JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        public CustomUserDetailsService customUserDetailsService() {
            return Mockito.mock(CustomUserDetailsService.class);
        }

        @Bean
        public UserRepository userRepository() {
            return Mockito.mock(UserRepository.class);
        }

        @Bean
        public ReportRepository reportRepository() {
            return Mockito.mock(ReportRepository.class);
        }

        @Bean
        public CategoryRepository categoryRepository() {
            return Mockito.mock(CategoryRepository.class);
        }
    }
}
