package com.cityguard.backend.controller;

import com.cityguard.backend.dto.admin.comment.AdminCommentResponse;
import com.cityguard.backend.dto.admin.comment.AdminUpdateCommentStatusRequest;
import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.dto.common.ErrorResponse;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.service.AdminCommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestController
@RequestMapping("/api/v1/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {
    private final AdminCommentService adminCommentService;
    @GetMapping
    public ResponseEntity<ApiResponse<AdminPageResponse<AdminCommentResponse>>> getcomments(
            @RequestParam(name = "reportPublicId", required = false) String reportPublicId,
            @RequestParam(name = "isHidden", required = false)Boolean isHidden,
            @RequestParam(name = "page", defaultValue = "0")int page,
            @RequestParam(name = "size", defaultValue = "10")int size
    ){
        var response = adminCommentService.getComments(reportPublicId, isHidden, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<AdminCommentResponse>> updateCommentStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody AdminUpdateCommentStatusRequest request
    ){
        var response = adminCommentService.updateCommentStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleInvalidUnput(){
        return ResponseEntity.badRequest().body(ErrorResponse.of(ErrorCode.INVALID_INPUT));
    }
}
