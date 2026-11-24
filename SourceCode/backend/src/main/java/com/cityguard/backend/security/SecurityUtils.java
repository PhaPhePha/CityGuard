package com.cityguard.backend.security;

import com.cityguard.backend.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Tiện ích lấy thông tin User hiện tại từ SecurityContext.
 */
public final class SecurityUtils {

    private SecurityUtils() {}

    public static Optional<User> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof CustomUserDetails userDetails) {
            return Optional.ofNullable(userDetails.getUser());
        }
        return Optional.empty();
    }

    public static Optional<Long> getCurrentUserId() {
        return getCurrentUser().map(User::getId);
    }
}

