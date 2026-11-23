package com.cityguard.backend.service;

import com.cityguard.backend.dto.comment.CommentRequest;
import com.cityguard.backend.dto.comment.CommentResponse;
import com.cityguard.backend.dto.common.PageResponse;

/**
 * Interface nghiệp vụ bình luận báo cáo sự cố (WBS Task 4.5.1).
 */
public interface CommentService {
    PageResponse<CommentResponse> getComments(String reportId, int page, int size);
    CommentResponse addComment(String reportId, CommentRequest request);
    void deleteComment(Long commentId);
}

