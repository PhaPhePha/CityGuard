package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.request.LoginRequest;
import com.cityguard.backend.dto.request.RefreshTokenRequest;
import com.cityguard.backend.dto.request.RegisterRequest;
import com.cityguard.backend.dto.response.AuthResponse;
import com.cityguard.backend.entity.RefreshToken;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.RefreshTokenRepository;
import com.cityguard.backend.repository.UserRepository;
import com.cityguard.backend.security.JwtService;
import com.cityguard.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Triển khai nghiệp vụ xác thực người dùng với JWT và Refresh Token Rotation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Tên đăng nhập đã được sử dụng");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Email đã được sử dụng");
        }

        OffsetDateTime now = OffsetDateTime.now();
        User user = User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName().trim())
                .role("USER")
                .status("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .build();

        user = userRepository.save(user);

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String account = request.getAccount().trim();

        User user;
        if (account.contains("@")) {
            user = userRepository.findByEmailIgnoreCase(account)
                    .orElseGet(() -> userRepository.findByUsernameIgnoreCase(account)
                            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không chính xác")));
        } else {
            user = userRepository.findByUsernameIgnoreCase(account)
                    .orElseGet(() -> userRepository.findByEmailIgnoreCase(account)
                            .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không chính xác")));
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không chính xác");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String rawRefreshToken = request.getRefreshToken().trim();
        String tokenHash = jwtService.hashToken(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Refresh token không hợp lệ hoặc không tồn tại"));

        if (storedToken.getRevokedAt() != null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Refresh token này đã bị thu hồi");
        }

        if (storedToken.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Refresh token đã hết hạn, vui lòng đăng nhập lại");
        }

        User user = storedToken.getUser();
        if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Tài khoản liên kết không tồn tại hoặc đã bị khóa");
        }

        // Token Rotation: Thu hồi token cũ
        storedToken.setRevokedAt(OffsetDateTime.now());
        refreshTokenRepository.save(storedToken);

        // Cấp cặp token mới
        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public void logout(RefreshTokenRequest request) {
        if (request != null && request.getRefreshToken() != null && !request.getRefreshToken().isBlank()) {
            String tokenHash = jwtService.hashToken(request.getRefreshToken().trim());
            refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(storedToken -> {
                if (storedToken.getRevokedAt() == null) {
                    storedToken.setRevokedAt(OffsetDateTime.now());
                    refreshTokenRepository.save(storedToken);
                }
            });
        }
    }

    private AuthResponse buildAuthResponse(User user) {
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

