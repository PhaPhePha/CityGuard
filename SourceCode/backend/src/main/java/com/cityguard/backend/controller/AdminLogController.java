package com.cityguard.backend.controller;

import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.dto.admin.log.AdminLogResponse;
import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.dto.common.ErrorResponse;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.service.AdminLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {
    private final AdminLogService adminLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<AdminPageResponse<AdminLogResponse>>> getLogs(
            @RequestParam(name = "actorId", required = false) Long actorid,
            @RequestParam(name = "action", required = false)String action,
            @RequestParam(name = "targetType", required = false)String targetType,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ){
        var response = adminLogService.getLogs(actorid,action,targetType,page,size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleInvalidParameter(){
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }
}
