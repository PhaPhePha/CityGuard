package com.cityguard.backend.controller;

import com.cityguard.backend.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Controller kiểm tra trạng thái hoạt động của hệ thống và kết nối Database (theo DacTaKyThuat Trang 8, Task 4.1.1 & DeploymentWorkAssignment TV5).
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Tag(name = "Health Check", description = "API giám sát trạng thái hoạt động của máy chủ backend và kết nối cơ sở dữ liệu")
public class HealthController {

    private final DataSource dataSource;

    @GetMapping
    @Operation(
            summary = "Kiểm tra sức khỏe hệ thống",
            description = "Kiểm tra trạng thái Application và thực hiện ping kết nối Database PostgreSQL"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        boolean dbUp = isDatabaseUp();

        Map<String, String> components = new LinkedHashMap<>();
        components.put("application", "UP");
        components.put("database", dbUp ? "UP" : "DOWN");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("status", dbUp ? "UP" : "DOWN");
        data.put("components", components);

        if (dbUp) {
            return ResponseEntity.ok(ApiResponse.success("Hệ thống hoạt động bình thường", data));
        }

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiResponse.failure("Dịch vụ Database không phản hồi", data));
    }

    private boolean isDatabaseUp() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(2);
            statement.execute("SELECT 1");
            return true;
        } catch (Exception ex) {
            log.warn("Health check kết nối Database thất bại: {}", ex.getMessage());
            return false;
        }
    }
}
