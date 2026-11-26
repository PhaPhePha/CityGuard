package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.comment.CommentRequest;
import com.cityguard.backend.dto.comment.CommentResponse;
import com.cityguard.backend.dto.common.PageResponse;
import com.cityguard.backend.entity.Comment;
import com.cityguard.backend.entity.Report;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.CommentRepository;
import com.cityguard.backend.repository.ReportRepository;
import com.cityguard.backend.security.SecurityUtils;
import com.cityguard.backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final ReportRepository reportRepository;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(String reportId, int page, int size) {
        Report report = findReportByIdOrPublicId(reportId);

        int pageSize = Math.min(Math.max(1, size), 50);
        int pageNumber = Math.max(0, page);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);

        Page<Comment> commentPage = commentRepository.findActiveCommentsByReportId(report.getId(), pageable);
        Long currentUserId = SecurityUtils.getCurrentUserId().orElse(null);

        Page<CommentResponse> responsePage = commentPage.map(c -> mapToCommentResponse(c, currentUserId));
        return PageResponse.of(responsePage);
    }

    @Override
    @Transactional
    public CommentResponse addComment(String reportId, CommentRequest request) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để bình luận"));

        Report report = findReportByIdOrPublicId(reportId);

        String body = request.getBody() != null ? request.getBody().trim() : "";
        if (body.isBlank()) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Nội dung bình luận không được để trống");
        }
        if (body.length() > 2000) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Nội dung bình luận tối đa 2000 ký tự");
        }

        UUID clientRequestId = request.getClientRequestId() != null ? request.getClientRequestId() : UUID.randomUUID();

        // Kiểm tra Idempotency theo client_request_id (BanGiaoDB_V2 Mục 7.2)
        Optional<Comment> existingOpt = commentRepository.findByClientRequestId(clientRequestId);
        if (existingOpt.isPresent()) {
            Comment existing = existingOpt.get();
            if (existing.getUser().getId().equals(currentUser.getId()) && existing.getBody().equals(body)) {
                return mapToCommentResponse(existing, currentUser.getId());
            } else {
                throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Yêu cầu bình luận bị trùng mã client_request_id với dữ liệu khác");
            }
        }

        OffsetDateTime now = OffsetDateTime.now();
        Comment comment = Comment.builder()
                .report(report)
                .user(currentUser)
                .clientRequestId(clientRequestId)
                .body(body)
                .isHidden(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        comment = commentRepository.save(comment);
        return mapToCommentResponse(comment, currentUser.getId());
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        User currentUser = SecurityUtils.getCurrentUser()
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để thực hiện"));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(currentUser.getId())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Bạn không có quyền xóa bình luận này");
        }

        // Soft delete (SRS Trang 9 FR-COM-04)
        comment.setDeletedAt(OffsetDateTime.now());
        commentRepository.save(comment);
    }

    private Report findReportByIdOrPublicId(String id) {
        if (id == null || id.isBlank()) {
            throw new AppException(ErrorCode.REPORT_NOT_FOUND);
        }
        String trimmed = id.trim();
        if (trimmed.matches("^\\d+$")) {
            Long numericId = Long.parseLong(trimmed);
            return reportRepository.findById(numericId)
                    .or(() -> reportRepository.findByPublicIdWithDetails(trimmed))
                    .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
        }
        return reportRepository.findByPublicIdWithDetails(trimmed)
                .orElseThrow(() -> new AppException(ErrorCode.REPORT_NOT_FOUND));
    }

    private CommentResponse mapToCommentResponse(Comment comment, Long currentUserId) {
        String createdAtStr = comment.getCreatedAt() != null
                ? DateTimeFormatter.ISO_INSTANT.format(comment.getCreatedAt().toInstant())
                : null;

        boolean isOwner = currentUserId != null && currentUserId.equals(comment.getUser().getId());

        return CommentResponse.builder()
                .id(String.valueOf(comment.getId()))
                .authorId(String.valueOf(comment.getUser().getId()))
                .authorName(comment.getUser().getDisplayName())
                .content(comment.getBody())
                .createdAt(createdAtStr)
                .isOwner(isOwner)
                .build();
    }
}

