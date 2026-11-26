package com.cityguard.backend;

import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.dto.auth.LoginRequest;
import com.cityguard.backend.dto.auth.RegisterRequest;
import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.dto.report.CreateReportRequest;
import com.cityguard.backend.enums.ReportType;
import com.cityguard.backend.repository.ReportRepository;
import com.cityguard.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Bộ kiểm thử Integration Test cho các API cốt lõi của CityGuard (DeploymentWorkAssignment Trang 4, Mục TV5).
 * Chạy trên PostgreSQL thực tế và tự động rollback sau mỗi test case bằng @Transactional.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CityGuardApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Test
    void testHealthCheck_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.components.application").value("UP"))
                .andExpect(jsonPath("$.data.components.database").value("UP"));
    }

    @Test
    void testGetReportDetail_NotFound_ShouldReturn404() throws Exception {
        String nonExistentId = "rpt_khong_ton_tai";
        assertTrue(reportRepository.findByPublicIdWithDetails(nonExistentId).isEmpty());

        String accessToken = registerAndGetAccessToken();

        mockMvc.perform(get("/api/v1/reports/{id}", nonExistentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("REPORT_NOT_FOUND"));
    }

    @Test
    void testCreateReport_WithoutToken_ShouldReturn401() throws Exception {
        CreateReportRequest request = CreateReportRequest.builder()
                .type(ReportType.TRAFFIC_JAM)
                .latitude(10.7769)
                .longitude(106.7009)
                .description("Kẹt xe thử nghiệm không có token")
                .build();

        mockMvc.perform(post("/api/v1/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"));
    }

    @Test
    void testCreateReport_InvalidInput_ShouldReturn400() throws Exception {
        String accessToken = registerAndGetAccessToken();

        // Thiếu type (null) và vĩ độ vượt quá [-90, 90]
        CreateReportRequest invalidRequest = CreateReportRequest.builder()
                .type(null)
                .latitude(999.0)
                .longitude(106.7009)
                .description("Dữ liệu đầu vào không hợp lệ")
                .build();

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("INVALID_INPUT"));
    }

    @Test
    void testAuth_RegisterAndLogin_HappyPath() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "cg_test_" + suffix;
        String email = username + "@cityguard.test";
        String password = "Password123!";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .username(username)
                .email(email)
                .password(password)
                .displayName("CityGuard Test User")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());

        assertTrue(userRepository.existsByUsernameIgnoreCase(username));

        LoginRequest loginRequest = LoginRequest.builder()
                .account(username)
                .password(password)
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
    }

    private String registerAndGetAccessToken() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "cg_user_" + suffix;
        String email = username + "@cityguard.test";

        RegisterRequest registerRequest = RegisterRequest.builder()
                .username(username)
                .email(email)
                .password("Password123!")
                .displayName("Integration Test User")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        ApiResponse<AuthResponse> apiResponse = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {}
        );
        return apiResponse.getData().getAccessToken();
    }
}
