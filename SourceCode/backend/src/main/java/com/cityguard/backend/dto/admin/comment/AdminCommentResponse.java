package com.cityguard.backend.dto.admin.comment;

import com.cityguard.backend.entity.Comment;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@AllArgsConstructor
public class AdminCommentResponse {
    private final Long id;
    private final String reportPublicId;
    private final Long userId;
    private final String username;
    private final String userDisplayName;
    private final String body;
    private final Boolean isHidden;
    private final String deletedAt;
    private final String createdAt;

    public static AdminCommentResponse from(Comment comment){
        var user = comment.getUser();
        return new AdminCommentResponse(
                comment.getId(),
                comment.getReport().getPublicId(),
                user.getId(),
                user.getUsername(),
                user.getDisplayName(),
                comment.getBody(),
                comment.getIsHidden(),
                formatTimestamp(comment.getDeletedAt()),
                formatTimestamp(comment.getCreatedAt())
        );
    }

    private static String formatTimestamp(OffsetDateTime timestamp){
        return timestamp == null
                ? null
                : DateTimeFormatter.ISO_INSTANT.format(timestamp.toInstant());
    }
}
