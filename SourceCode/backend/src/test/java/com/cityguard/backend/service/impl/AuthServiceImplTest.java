package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.dto.auth.LoginRequest;
import com.cityguard.backend.dto.auth.RefreshTokenRequest;
import com.cityguard.backend.dto.auth.RegisterRequest;
import com.cityguard.backend.entity.RefreshToken;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.RefreshTokenRepository;
import com.cityguard.backend.repository.UserRepository;
import com.cityguard.backend.security.AuthTokenIssuer;
import com.cityguard.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthTokenIssuer authTokenIssuer;

    @InjectMocks
    private AuthServiceImpl authService;

    private User activeUser;
    private AuthResponse stubAuthResponse;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .username("demo_citizen")
                .email("citizen@example.invalid")
                .passwordHash("$2a$10$hashedPassword")
                .displayName("Demo Citizen")
                .role("USER")
                .status("ACTIVE")
                .build();

        stubAuthResponse = AuthResponse.builder()
                .accessToken("access-token-123")
                .refreshToken("refresh-token-456")
                .tokenType("Bearer")
                .expiresIn(3600000L)
                .userInfo(AuthResponse.UserInfo.builder()
                        .id(1L)
                        .username("demo_citizen")
                        .email("citizen@example.invalid")
                        .displayName("Demo Citizen")
                        .role("USER")
                        .build())
                .build();
    }

    @Test
    void register_success_shouldSaveUserAndDelegateToAuthTokenIssuer() {
        RegisterRequest request = RegisterRequest.builder()
                .username("  new_user ")
                .email(" NewUser@Example.com ")
                .password("Password123!")
                .displayName(" New User ")
                .build();

        when(userRepository.existsByUsernameIgnoreCase("new_user")).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123!")).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(99L);
            return u;
        });
        when(authTokenIssuer.issueAuthResponse(any(User.class))).thenReturn(stubAuthResponse);

        AuthResponse result = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();

        assertThat(saved.getUsername()).isEqualTo("new_user");
        assertThat(saved.getEmail()).isEqualTo("newuser@example.com");
        assertThat(saved.getRole()).isEqualTo("USER");
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        verify(authTokenIssuer).issueAuthResponse(saved);
        assertThat(result).isSameAs(stubAuthResponse);
    }

    @Test
    void register_duplicateUsername_shouldThrowConflictAndNotIssueToken() {
        RegisterRequest request = RegisterRequest.builder()
                .username("demo_citizen")
                .email("other@example.com")
                .password("Password123!")
                .displayName("Demo")
                .build();

        when(userRepository.existsByUsernameIgnoreCase("demo_citizen")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.RESOURCE_CONFLICT);

        verifyNoInteractions(authTokenIssuer);
    }

    @Test
    void login_withUsername_success_shouldDelegateToAuthTokenIssuer() {
        LoginRequest request = LoginRequest.builder()
                .account("demo_citizen")
                .password("SamplePassword123!")
                .build();

        when(userRepository.findByUsernameIgnoreCase("demo_citizen")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SamplePassword123!", activeUser.getPasswordHash())).thenReturn(true);
        when(authTokenIssuer.issueAuthResponse(activeUser)).thenReturn(stubAuthResponse);

        AuthResponse response = authService.login(request);

        assertThat(response).isSameAs(stubAuthResponse);
        verify(authTokenIssuer).issueAuthResponse(activeUser);
    }

    @Test
    void login_withEmail_success_shouldDelegateToAuthTokenIssuer() {
        LoginRequest request = LoginRequest.builder()
                .account("citizen@example.invalid")
                .password("SamplePassword123!")
                .build();

        when(userRepository.findByEmailIgnoreCase("citizen@example.invalid")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("SamplePassword123!", activeUser.getPasswordHash())).thenReturn(true);
        when(authTokenIssuer.issueAuthResponse(activeUser)).thenReturn(stubAuthResponse);

        AuthResponse response = authService.login(request);

        assertThat(response).isSameAs(stubAuthResponse);
        verify(authTokenIssuer).issueAuthResponse(activeUser);
    }

    @Test
    void login_lockedAccount_shouldThrowForbiddenAndNotIssueToken() {
        User lockedUser = User.builder()
                .id(3L)
                .username("demo_locked")
                .email("locked@example.invalid")
                .passwordHash("$2a$10$hashed")
                .role("USER")
                .status("LOCKED")
                .build();

        LoginRequest request = LoginRequest.builder()
                .account("demo_locked")
                .password("SamplePassword123!")
                .build();

        when(userRepository.findByUsernameIgnoreCase("demo_locked")).thenReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verifyNoInteractions(authTokenIssuer);
    }

    @Test
    void login_wrongPassword_shouldThrowUnauthorizedAndNotIssueToken() {
        LoginRequest request = LoginRequest.builder()
                .account("demo_citizen")
                .password("WrongPassword")
                .build();

        when(userRepository.findByUsernameIgnoreCase("demo_citizen")).thenReturn(Optional.of(activeUser));
        when(passwordEncoder.matches("WrongPassword", activeUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verifyNoInteractions(authTokenIssuer);
    }

    @Test
    void login_unknownAccount_shouldThrowUnauthorizedAndNotIssueToken() {
        LoginRequest request = LoginRequest.builder()
                .account("nonexistent")
                .password("SamplePassword123!")
                .build();

        when(userRepository.findByUsernameIgnoreCase("nonexistent")).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verifyNoInteractions(authTokenIssuer);
    }

    @Test
    void refreshToken_valid_shouldRevokeOldTokenAndDelegateToAuthTokenIssuer() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("raw-refresh-token")
                .build();

        RefreshToken storedToken = RefreshToken.builder()
                .id(100L)
                .user(activeUser)
                .tokenHash("hashed-token")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revokedAt(null)
                .build();

        when(jwtService.hashToken("raw-refresh-token")).thenReturn("hashed-token");
        when(refreshTokenRepository.findByTokenHash("hashed-token")).thenReturn(Optional.of(storedToken));
        when(authTokenIssuer.issueAuthResponse(activeUser)).thenReturn(stubAuthResponse);

        AuthResponse response = authService.refreshToken(request);

        assertThat(storedToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(storedToken);
        verify(authTokenIssuer).issueAuthResponse(activeUser);
        assertThat(response).isSameAs(stubAuthResponse);
    }

    @Test
    void refreshToken_revoked_shouldThrowUnauthorizedAndNotIssueToken() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("revoked-token")
                .build();

        RefreshToken storedToken = RefreshToken.builder()
                .id(101L)
                .user(activeUser)
                .tokenHash("hashed-revoked")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revokedAt(OffsetDateTime.now().minusMinutes(5))
                .build();

        when(jwtService.hashToken("revoked-token")).thenReturn("hashed-revoked");
        when(refreshTokenRepository.findByTokenHash("hashed-revoked")).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verifyNoInteractions(authTokenIssuer);
    }

    @Test
    void logout_activeRefreshToken_shouldMarkRevokedAt() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("raw-token-to-logout")
                .build();

        RefreshToken storedToken = RefreshToken.builder()
                .id(102L)
                .user(activeUser)
                .tokenHash("hashed-logout")
                .expiresAt(OffsetDateTime.now().plusDays(1))
                .revokedAt(null)
                .build();

        when(jwtService.hashToken("raw-token-to-logout")).thenReturn("hashed-logout");
        when(refreshTokenRepository.findByTokenHash("hashed-logout")).thenReturn(Optional.of(storedToken));

        authService.logout(request);

        assertThat(storedToken.getRevokedAt()).isNotNull();
        verify(refreshTokenRepository).save(storedToken);
        verify(authTokenIssuer, never()).issueAuthResponse(any());
    }
}
