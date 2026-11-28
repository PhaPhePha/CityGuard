package com.cityguard.backend.security;

import com.cityguard.backend.controller.AdminAuthController;
import com.cityguard.backend.controller.AuthController;
import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.dto.auth.LoginRequest;
import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.exception.GlobalExceptionHandler;
import com.cityguard.backend.service.AdminAuthService;
import com.cityguard.backend.service.AuthService;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = AdminSecurityConfigTest.TestSecurityWebConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=CityGuardTestSecretKeyWithAtLeast32BytesLengthForHS256!",
        "jwt.access-expiration-ms=3600000",
        "jwt.refresh-expiration-ms=604800000"
})
class AdminSecurityConfigTest {

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
    private AdminAuthService adminAuthService;

    @Autowired
    private AuthService authService;

    private MockMvc mockMvc;

    private User adminUser;
    private User moderatorUser;
    private User citizenUser;

    @BeforeEach
    void setUp() {
        reset(userDetailsService, adminAuthService, authService);
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
    @DisplayName("1. POST /api/v1/admin/auth/login có permitAll (không cần JWT) và gọi đúng AdminAuthService.adminLogin")
    void postAdminLogin_withoutJwt_shouldBePermittedAndCallAdminAuthService() throws Exception {
        AuthResponse stubResponse = AuthResponse.builder()
                .accessToken("admin-token")
                .tokenType("Bearer")
                .userInfo(AuthResponse.UserInfo.builder()
                        .id(4L)
                        .username("demo_admin")
                        .email("admin@example.invalid")
                        .displayName("Demo Admin")
                        .role("ADMIN")
                        .build())
                .build();

        when(adminAuthService.adminLogin(any(LoginRequest.class))).thenReturn(stubResponse);

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"admin@example.invalid\",\"password\":\"SamplePassword123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(adminAuthService).adminLogin(any(LoginRequest.class));
    }

    @Test
    @DisplayName("2. Request vào /api/v1/admin/** không có JWT hoặc JWT sai bị từ chối 401 UNAUTHORIZED")
    void adminEndpoints_withoutJwtOrWithInvalidJwt_shouldReturn401() throws Exception {
        // Không có JWT
        mockMvc.perform(get("/api/v1/admin/dashboard/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));

        // JWT sai định dạng / chữ ký không hợp lệ
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("3. JWT ROLE_USER không thể truy cập bất kỳ API /api/v1/admin/** nào (403 FORBIDDEN)")
    void adminEndpoints_withRoleUserJwt_shouldReturn403Forbidden() throws Exception {
        String userToken = jwtService.generateAccessToken(citizenUser);

        mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/admin/logs")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("4. MODERATOR truy cập được /api/v1/admin/dashboard/summary nhưng bị chặn 403 ở /api/v1/admin/users và /api/v1/admin/logs")
    void moderatorAccess_shouldAllowGeneralAdminApisAndDenyUsersAndLogs() throws Exception {
        String modToken = jwtService.generateAccessToken(moderatorUser);

        // Được phép truy cập API quản trị chung (dashboard, reports, comments, categories)
        mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + modToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("dashboard-ok"));

        // Bị chặn 403 ở /api/v1/admin/users và /api/v1/admin/users/**
        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + modToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        mockMvc.perform(patch("/api/v1/admin/users/1/status")
                        .header("Authorization", "Bearer " + modToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));

        // Bị chặn 403 ở /api/v1/admin/logs và /api/v1/admin/logs/**
        mockMvc.perform(get("/api/v1/admin/logs")
                        .header("Authorization", "Bearer " + modToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    @DisplayName("5. ADMIN có thể truy cập đầy đủ tất cả API quản trị (/dashboard, /users, /logs)")
    void adminAccess_shouldAllowAllAdminApis() throws Exception {
        String adminToken = jwtService.generateAccessToken(adminUser);

        mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("dashboard-ok"));

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("users-ok"));

        mockMvc.perform(patch("/api/v1/admin/users/1/status")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("user-status-updated"));

        mockMvc.perform(get("/api/v1/admin/logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("logs-ok"));
    }

    @Test
    @DisplayName("6. API Android hiện có (/api/v1/auth/login, /api/v1/health) không bị ảnh hưởng")
    void androidApis_shouldRemainUnaffected() throws Exception {
        AuthResponse stubResponse = AuthResponse.builder()
                .accessToken("user-token")
                .refreshToken("user-refresh")
                .tokenType("Bearer")
                .expiresIn(3600000L)
                .build();

        when(authService.login(any(LoginRequest.class))).thenReturn(stubResponse);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"account\":\"demo_citizen\",\"password\":\"SamplePassword123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").value("user-token"));

        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("7. Kiểm tra tương thích DTO với React Admin Web (request gửi trường 'identifier', response trả 'data.token' và 'data.user.status')")
    void reactAdminWebContractCompatibility_shouldAcceptIdentifierAndReturnTokenAndUser() throws Exception {
        AuthResponse stubResponse = AuthResponse.builder()
                .accessToken("admin-jwt-token")
                .tokenType("Bearer")
                .userInfo(AuthResponse.UserInfo.builder()
                        .id(4L)
                        .username("demo_admin")
                        .email("admin@example.invalid")
                        .displayName("Demo Admin")
                        .role("ADMIN")
                        .build())
                .build();

        when(adminAuthService.adminLogin(any(LoginRequest.class))).thenReturn(stubResponse);

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"admin@example.invalid\",\"password\":\"SamplePassword123!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").value("admin-jwt-token"))
                .andExpect(jsonPath("$.data.user.username").value("demo_admin"))
                .andExpect(jsonPath("$.data.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.user.status").value("ACTIVE"));
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
            AdminAuthController.class,
            AuthController.class,
            DummyProtectedEndpointsController.class
    })
    static class TestSecurityWebConfig {

        @Bean
        public JsonMapper jsonMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        public CustomUserDetailsService customUserDetailsService() {
            return Mockito.mock(CustomUserDetailsService.class);
        }

        @Bean
        public AdminAuthService adminAuthService() {
            return Mockito.mock(AdminAuthService.class);
        }

        @Bean
        public AuthService authService() {
            return Mockito.mock(AuthService.class);
        }
    }

    @RestController
    static class DummyProtectedEndpointsController {

        @GetMapping("/api/v1/health")
        public ApiResponse<String> health() {
            return ApiResponse.success("health-ok");
        }

        @GetMapping("/api/v1/admin/dashboard/summary")
        public ApiResponse<String> dashboard() {
            return ApiResponse.success("dashboard-ok");
        }

        @GetMapping("/api/v1/admin/users")
        public ApiResponse<String> users() {
            return ApiResponse.success("users-ok");
        }

        @PatchMapping("/api/v1/admin/users/1/status")
        public ApiResponse<String> updateUserStatus() {
            return ApiResponse.success("user-status-updated");
        }

        @GetMapping("/api/v1/admin/logs")
        public ApiResponse<String> logs() {
            return ApiResponse.success("logs-ok");
        }
    }
}
