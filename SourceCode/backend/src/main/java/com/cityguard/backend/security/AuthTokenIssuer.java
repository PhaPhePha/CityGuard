package com.cityguard.backend.security;

import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.entity.RefreshToken;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Component dùng chung chịu trách nhiệm phát hành JWT Access Token,
 * khởi tạo & lưu Refresh Token (SHA-256 hash) vào CSDL và đóng gói AuthResponse.
 */
@Component
@RequiredArgsConstructor
public class AuthTokenIssuer {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthResponse issueAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = jwtService.generateRawRefreshToken();
        String tokenHash = jwtService.hashToken(rawRefreshToken);

        OffsetDateTime expiresAt = OffsetDateTime.now().plus(jwtService.getRefreshExpirationMs(), ChronoUnit.MILLIS);
        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(rawRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessExpirationMs())
                .userInfo(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .displayName(user.getDisplayName())
                        .role(user.getRole())
                        .build())
                .build();
    }
}
