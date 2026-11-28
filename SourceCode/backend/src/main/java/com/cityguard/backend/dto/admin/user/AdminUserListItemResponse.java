package com.cityguard.backend.dto.admin.user;

import com.cityguard.backend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@AllArgsConstructor
public class AdminUserListItemResponse {
    private final Long id;
    private final String username;
    private final String email;
    private final String displayName;
    private final String avatarUrl;
    private final String role;
    private final String status;
    private final String createdAt;

    public static AdminUserListItemResponse from(User user){
        String createdAt = user.getCreatedAt() == null
                ?null
                : DateTimeFormatter.ISO_INSTANT.format(user.getCreatedAt().toInstant());

        return new AdminUserListItemResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getDisplayName(),
                user.getAvatarUrl(),
                user.getRole(),
                user.getStatus(),
                createdAt
        );
    }
}
