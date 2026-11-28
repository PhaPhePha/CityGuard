package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.dto.auth.FirebaseLoginRequest;
import com.cityguard.backend.dto.auth.LoginRequest;
import com.cityguard.backend.dto.auth.RefreshTokenRequest;
import com.cityguard.backend.dto.auth.RegisterRequest;
import com.cityguard.backend.entity.RefreshToken;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.entity.UserAuthProvider;
import com.cityguard.backend.entity.UserFirebaseAccount;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.RefreshTokenRepository;
import com.cityguard.backend.repository.UserAuthProviderRepository;
import com.cityguard.backend.repository.UserFirebaseAccountRepository;
import com.cityguard.backend.repository.UserRepository;
import com.cityguard.backend.security.AuthTokenIssuer;
import com.cityguard.backend.security.JwtService;
import com.cityguard.backend.service.AuthService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Triển khai nghiệp vụ xác thực người dùng với JWT và Refresh Token Rotation.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final UserFirebaseAccountRepository userFirebaseAccountRepository;
    private final UserAuthProviderRepository userAuthProviderRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthTokenIssuer authTokenIssuer;

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

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không chính xác");
        }

        return buildAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithFirebase(FirebaseLoginRequest request) {
        String provider = request.getProvider().trim().toUpperCase(Locale.ROOT);
        String expectedProviderId = switch (provider) {
            case "GOOGLE" -> "google.com";
            case "FACEBOOK" -> "facebook.com";
            default -> throw new AppException(ErrorCode.INVALID_INPUT, "Provider chỉ chấp nhận GOOGLE hoặc FACEBOOK");
        };

        FirebaseToken decodedToken;
        try {
            decodedToken = FirebaseAuth.getInstance().verifyIdToken(request.getIdToken().trim());
        } catch (FirebaseAuthException ex) {
            log.warn("Xác thực Firebase ID token thất bại: {}", ex.getMessage());
            throw new AppException(ErrorCode.UNAUTHORIZED, "Firebase ID token không hợp lệ hoặc đã hết hạn");
        }

        Map<String, Object> claims = decodedToken.getClaims() != null ? decodedToken.getClaims() : Collections.emptyMap();
        Object firebaseClaimObj = claims.get("firebase");
        Map<?, ?> firebaseClaim = firebaseClaimObj instanceof Map<?, ?> map ? map : Collections.emptyMap();

        String signInProvider = firebaseClaim.get("sign_in_provider") instanceof String s ? s : null;
        if (!expectedProviderId.equals(signInProvider)) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Nhà cung cấp đăng nhập không khớp với Firebase token");
        }

        String firebaseUid = decodedToken.getUid();
        if (firebaseUid == null || firebaseUid.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Firebase token thiếu thông tin định danh người dùng");
        }

        String providerUid = extractProviderUid(firebaseClaim, expectedProviderId, firebaseUid);
        String email = normalizeBlankToNull(decodedToken.getEmail());
        if (email != null) {
            email = email.toLowerCase(Locale.ROOT);
        }
        String displayName = truncate(normalizeBlankToNull(decodedToken.getName()), 100);
        String avatarUrl = normalizeBlankToNull(decodedToken.getPicture());

        OffsetDateTime now = OffsetDateTime.now();
        UserFirebaseAccount existingFirebase = userFirebaseAccountRepository.findByFirebaseUid(firebaseUid).orElse(null);

        User user;
        if (existingFirebase != null) {
            user = existingFirebase.getUser();
            if ("LOCKED".equalsIgnoreCase(user.getStatus()) || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                throw new AppException(ErrorCode.FORBIDDEN, "Tài khoản của bạn đã bị khóa hoặc ngừng hoạt động");
            }

            boolean userModified = false;
            if (displayName != null && !displayName.equals(user.getDisplayName())) {
                user.setDisplayName(displayName);
                userModified = true;
            }
            if (avatarUrl != null && !avatarUrl.equals(user.getAvatarUrl())) {
                user.setAvatarUrl(avatarUrl);
                userModified = true;
            }
            if (userModified) {
                user.setUpdatedAt(now);
                user = userRepository.save(user);
            }

            UserAuthProvider existingProvider = userAuthProviderRepository
                    .findByUserIdAndProvider(user.getId(), provider)
                    .orElse(null);
            if (existingProvider != null) {
                if (email != null) {
                    existingProvider.setEmail(email);
                }
                if (displayName != null) {
                    existingProvider.setDisplayName(displayName);
                }
                if (avatarUrl != null) {
                    existingProvider.setAvatarUrl(avatarUrl);
                }
                existingProvider.setUpdatedAt(now);
                userAuthProviderRepository.save(existingProvider);
            } else {
                if (userAuthProviderRepository.existsByProviderAndProviderUid(provider, providerUid)) {
                    throw new AppException(ErrorCode.RESOURCE_CONFLICT, "Tài khoản mạng xã hội này đã được liên kết với người dùng khác");
                }
                UserAuthProvider newProvider = UserAuthProvider.builder()
                        .user(existingFirebase.getUser())
                        .firebaseAccount(existingFirebase)
                        .provider(provider)
                        .providerUid(providerUid)
                        .email(email)
                        .displayName(displayName)
                        .avatarUrl(avatarUrl)
                        .createdAt(now)
                        .updatedAt(now)
                        .build();
                userAuthProviderRepository.save(newProvider);
            }
        } else {
            if (email != null && userRepository.existsByEmailIgnoreCase(email)) {
                throw new AppException(
                        ErrorCode.RESOURCE_CONFLICT,
                        "Email này đã được đăng ký bằng mật khẩu, vui lòng đăng nhập thường để sử dụng"
                );
            }
            if (userAuthProviderRepository.existsByProviderAndProviderUid(provider, providerUid)) {
                throw new AppException(
                        ErrorCode.RESOURCE_CONFLICT,
                        "Tài khoản mạng xã hội này đã được liên kết với người dùng khác"
                );
            }

            String username = generateUniqueUsername(email, firebaseUid);
            String resolvedDisplayName = displayName != null ? displayName : username;

            user = User.builder()
                    .username(username)
                    .email(email)
                    .passwordHash(null)
                    .displayName(resolvedDisplayName)
                    .avatarUrl(avatarUrl)
                    .role("USER")
                    .status("ACTIVE")
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            user = userRepository.save(user);

            UserFirebaseAccount firebaseAccount = UserFirebaseAccount.builder()
                    .user(user)
                    .firebaseUid(firebaseUid)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            firebaseAccount = userFirebaseAccountRepository.save(firebaseAccount);

            UserAuthProvider authProvider = UserAuthProvider.builder()
                    .user(user)
                    .firebaseAccount(firebaseAccount)
                    .provider(provider)
                    .providerUid(providerUid)
                    .email(email)
                    .displayName(displayName)
                    .avatarUrl(avatarUrl)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            userAuthProviderRepository.save(authProvider);
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
        return authTokenIssuer.issueAuthResponse(user);
    }

    private String extractProviderUid(Map<?, ?> firebaseClaim, String providerId, String fallbackUid) {
        Object identitiesObj = firebaseClaim.get("identities");
        if (identitiesObj instanceof Map<?, ?> identities) {
            Object providerIdsObj = identities.get(providerId);
            if (providerIdsObj instanceof List<?> providerIds && !providerIds.isEmpty() && providerIds.get(0) != null) {
                String firstId = providerIds.get(0).toString().trim();
                if (!firstId.isEmpty()) {
                    return firstId;
                }
            }
        }
        return fallbackUid;
    }

    private String generateUniqueUsername(String email, String firebaseUid) {
        String rawBase;
        if (email != null && email.contains("@")) {
            rawBase = email.substring(0, email.indexOf('@'));
        } else {
            rawBase = "user_" + firebaseUid;
        }

        String sanitized = rawBase.replaceAll("[^A-Za-z0-9_]", "_");
        if (sanitized.length() < 3) {
            sanitized = "user_" + sanitized;
        }
        if (sanitized.length() > 40) {
            sanitized = sanitized.substring(0, 40);
        }

        String candidate = sanitized;
        if (!userRepository.existsByUsernameIgnoreCase(candidate)) {
            return candidate;
        }

        for (int i = 0; i < 10; i++) {
            String suffix = "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
            String withSuffix = (sanitized.length() > 41 ? sanitized.substring(0, 41) : sanitized) + suffix;
            if (!userRepository.existsByUsernameIgnoreCase(withSuffix)) {
                return withSuffix;
            }
        }

        return "user_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private String normalizeBlankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
