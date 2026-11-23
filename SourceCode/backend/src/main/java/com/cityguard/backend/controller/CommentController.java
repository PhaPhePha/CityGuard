package com.cityguard.backend.controller;

import com.cityguard.backend.dto.common.ApiResponse;
import com.cityguard.backend.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller quản lý bình luận (theo SRS Trang 9 FR-COM-04).
 */
@RestController
@RequestMapping("/api/v1/comments")
@RequiredArgsConstructor
@Tag(name = "Comments Management", description = "Các API quản lý và thao tác bình luận")
public class CommentController {

    private final CommentService commentService;

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Xóa bình luận", description = "Chủ bình luận thực hiện xóa mềm (soft delete) bình luận của mình")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "Mã ID của bình luận cần xóa")
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(ApiResponse.success("Xóa bình luận thành công", null));
    }
}

