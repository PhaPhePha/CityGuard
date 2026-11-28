package com.cityguard.backend.service;

import com.cityguard.backend.dto.admin.comment.AdminCommentResponse;
import com.cityguard.backend.dto.admin.comment.AdminUpdateCommentStatusRequest;
import com.cityguard.backend.dto.admin.common.AdminPageResponse;

public interface AdminCommentService {
    AdminPageResponse<AdminCommentResponse> getComments(
            String reportPublicId,
            Boolean isHidden,
            int page,
            int size
    );
    AdminCommentResponse updateCommentStatus(
            Long id,
            AdminUpdateCommentStatusRequest request
    );
}
