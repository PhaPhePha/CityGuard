package com.cityguard.backend.controller;

import com.cityguard.backend.dto.admin.dashboard.AdminDashboardSummaryResponse;
import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {
    private final AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
        public ResponseEntity<ApiResponse<AdminDashboardSummaryResponse>> getSummary(){
            AdminDashboardSummaryResponse response =
                    adminDashboardService.getSummary();
            return ResponseEntity.ok(
                    ApiResponse.success(response)
            );
        }
}
