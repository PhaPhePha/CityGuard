package com.cityguard.backend.controller;

import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.dto.admin.user.AdminUpdateUserStatusRequest;
import com.cityguard.backend.dto.admin.user.AdminUserDetailResponse;
import com.cityguard.backend.dto.admin.user.AdminUserListItemResponse;
import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.dto.common.ErrorResponse;
import com.cityguard.backend.service.AdminUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;
    @GetMapping
    public ResponseEntity<ApiResponse<AdminPageResponse<AdminUserListItemResponse>>> getUsers(
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "role",required = false) String role,
            @RequestParam(name = "status", required = false)String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10")int size
    ){
        var response = adminUserService.getUsers(search, role, status,page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> getUserDetail(
            @PathVariable("id") Long id
    ){
        return ResponseEntity.ok(ApiResponse.success(adminUserService.getUserDetail(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminUserListItemResponse>> updateUserStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminUpdateUserStatusRequest request
    ){
      var response = adminUserService.updateUserStatus(id, request);
      return ResponseEntity.ok(ApiResponse.success(response));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex){
        String errorcode = HttpStatus.NOT_FOUND.equals(ex.getStatusCode())
                ? "USER_NOT_FOUND"
                : "HTTP_" + ex.getStatusCode().value();
        var body = ErrorResponse.builder()
                .errorCode(errorcode)
                .message(ex.getReason())
                .timestamp(Instant.now())
                .build();

        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
}
