package com.cityguard.backend.controller;

import com.cityguard.backend.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller kiểm tra trạng thái hoạt động của hệ thống (theo DacTaKyThuat Trang 8, Task 4.1.1).
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "API giám sát trạng thái hoạt động của máy chủ backend")
public class HealthController {

    @GetMapping
    @Operation(summary = "Kiểm tra sức khỏe hệ thống", description = "Trả về trạng thái hoạt động UP của dịch vụ")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkHealth() {
        return ResponseEntity.ok(ApiResponse.success("Hệ thống hoạt động bình thường", Map.of("status", "UP")));
    }
}
