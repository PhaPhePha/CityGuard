package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.admin.comment.AdminCommentResponse;
import com.cityguard.backend.dto.admin.comment.AdminUpdateCommentStatusRequest;
import com.cityguard.backend.dto.admin.common.AdminPageResponse;
import com.cityguard.backend.entity.AdminLog;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.security.SecurityUtils;
import com.cityguard.backend.service.AdminCommentService;
import com.cityguard.backend.repository.AdminLogRepository;
import com.cityguard.backend.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AdminCommentServiceImpl implements AdminCommentService {
    private final CommentRepository commentRepository;
    private final AdminLogRepository adminLogRepository;

    @Override
    @Transactional(readOnly = true)
    public AdminPageResponse<AdminCommentResponse> getComments(
            String reportPublicId,
            Boolean isHidden,
            int page,
            int size
    ){
        if(page <0 || size <1 || size >100){
            throw new AppException(ErrorCode.INVALID_INPUT, "Thông số phân trang không hợp lệ");
        }
        String publicId = reportPublicId == null || reportPublicId.isBlank()
                ?null
                : reportPublicId.strip();
        var comments = commentRepository.findAdminComments(
                publicId, isHidden, PageRequest.of(page, size)
        );
        return AdminPageResponse.of(comments.map(AdminCommentResponse::from));
    }

    @Override
    @Transactional
    public AdminCommentResponse updateCommentStatus(
            Long id,
            AdminUpdateCommentStatusRequest request
    ){
        var actor = SecurityUtils.getCurrentUser()
                .orElseThrow(()->new AppException(ErrorCode.UNAUTHORIZED));

        Boolean target = request.getIsHidden();
        if(target == null){
            throw new AppException(ErrorCode.INVALID_INPUT,"Trạng thái ẩn bình luận là bắt buộc");
        }
        var comment = commentRepository.findByIdForUpdate(id)
                .orElseThrow(()-> new AppException(ErrorCode.COMMENT_NOT_FOUND));

        if(target.equals(comment.getIsHidden())){
            return AdminCommentResponse.from(comment);
        }

        comment.setIsHidden(target);
        adminLogRepository.save(AdminLog.builder()
                .actor(actor)
                .action(target ? "HIDE_COMMENT" : "RESTORE_COMMENT")
                .targetType("COMMENT")
                .targetId(comment.getId())
                .reason(request.getReason())
                .createdAt(OffsetDateTime.now())
                .build()
        );
        return AdminCommentResponse.from(comment);
    }
}
