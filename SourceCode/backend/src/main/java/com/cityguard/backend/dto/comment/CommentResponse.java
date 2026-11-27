package com.cityguard.backend.dto.comment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO dữ liệu bình luận trả về client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private String id;
    private String authorName;
    private String authorId;
    private String body;
    private String content;
    private String createdAt;
    private boolean isOwner;
}
