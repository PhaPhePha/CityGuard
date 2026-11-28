package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.dto.auth.LoginRequest;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.enums.ErrorCode;
import com.cityguard.backend.exception.AppException;
import com.cityguard.backend.repository.UserRepository;
import com.cityguard.backend.security.AuthTokenIssuer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthTokenIssuer authTokenIssuer;

    @InjectMocks
    private AdminAuthServiceImpl adminAuthService;

    private User adminUser;
    private User moderatorUser;
    private User citizenUser;
    private User lockedAdminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(4L)
                .username("demo_admin")
                .email("admin@example.invalid")
                .passwordHash("$2a$10$adminHash")
                .displayName("Demo Admin")
                .role("ADMIN")
                .status("ACTIVE")
                .build();

        moderatorUser = User.builder()
                .id(5L)
                .username("demo_moderator")
                .email("mod@example.invalid")
                .passwordHash("$2a$10$modHash")
                .displayName("Demo Moderator")
                .role("MODERATOR")
                .status("ACTIVE")
                .build();

        citizenUser = User.builder()
                .id(1L)
                .username("demo_citizen")
                .email("citizen@example.invalid")
                .passwordHash("$2a$10$userHash")
                .displayName("Demo Citizen")
                .role("USER")
                .status("ACTIVE")
                .build();

        lockedAdminUser = User.builder()
                .id(6L)
                .username("locked_admin")
                .email("locked_admin@example.invalid")
                .passwordHash("$2a$10$lockedHash")
                .displayName("Locked Admin")
                .role("ADMIN")
                .status("LOCKED")
                .build();
    }

    @Test
    @DisplayName("1. ADMIN đăng nhập bằng Email thành công -> cấp token")
    void adminLogin_withAdminEmail_shouldIssueToken() {
        LoginRequest request = LoginRequest.builder()
                .account("admin@example.invalid")
                .password("SamplePassword123!")
                .build();

        AuthResponse expected = AuthResponse.builder().accessToken("admin-jwt").build();
        when(userRepository.findByEmailIgnoreCase("admin@example.invalid")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("SamplePassword123!", adminUser.getPasswordHash())).thenReturn(true);
        when(authTokenIssuer.issueAuthResponse(adminUser)).thenReturn(expected);

        AuthResponse actual = adminAuthService.adminLogin(request);

        assertThat(actual).isSameAs(expected);
        verify(authTokenIssuer).issueAuthResponse(adminUser);
    }

    @Test
    @DisplayName("2. ADMIN đăng nhập bằng Username (demo_admin) thành công -> cấp token")
    void adminLogin_withAdminUsername_shouldIssueToken() {
        LoginRequest request = LoginRequest.builder()
                .account("demo_admin")
                .password("SamplePassword123!")
                .build();

        AuthResponse expected = AuthResponse.builder().accessToken("admin-jwt").build();
        lenient().when(userRepository.findByUsernameIgnoreCase("demo_admin")).thenReturn(Optional.of(adminUser));
        lenient().when(userRepository.findByEmailIgnoreCase("demo_admin")).thenReturn(Optional.empty());
        lenient().when(passwordEncoder.matches("SamplePassword123!", adminUser.getPasswordHash())).thenReturn(true);
        lenient().when(authTokenIssuer.issueAuthResponse(adminUser)).thenReturn(expected);

        AuthResponse actual = adminAuthService.adminLogin(request);

        assertThat(actual).isSameAs(expected);
        verify(authTokenIssuer).issueAuthResponse(adminUser);
    }

    @Test
    @DisplayName("3. MODERATOR (ACTIVE) đăng nhập Admin thành công -> cấp token")
    void adminLogin_withModerator_shouldIssueToken() {
        LoginRequest request = LoginRequest.builder()
                .account("mod@example.invalid")
                .password("SamplePassword123!")
                .build();

        AuthResponse expected = AuthResponse.builder().accessToken("mod-jwt").build();
        when(userRepository.findByEmailIgnoreCase("mod@example.invalid")).thenReturn(Optional.of(moderatorUser));
        when(passwordEncoder.matches("SamplePassword123!", moderatorUser.getPasswordHash())).thenReturn(true);
        lenient().when(authTokenIssuer.issueAuthResponse(moderatorUser)).thenReturn(expected);

        AuthResponse actual = adminAuthService.adminLogin(request);

        assertThat(actual).isSameAs(expected);
        verify(authTokenIssuer).issueAuthResponse(moderatorUser);
    }

    @Test
    @DisplayName("4. Tài khoản USER không thể đăng nhập Admin -> ném 403 FORBIDDEN và KHÔNG cấp refresh token")
    void adminLogin_withRoleUser_shouldThrowForbiddenAndNotIssueRefreshToken() {
        LoginRequest request = LoginRequest.builder()
                .account("citizen@example.invalid")
                .password("SamplePassword123!")
                .build();

        when(userRepository.findByEmailIgnoreCase("citizen@example.invalid")).thenReturn(Optional.of(citizenUser));
        when(passwordEncoder.matches("SamplePassword123!", citizenUser.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> adminAuthService.adminLogin(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verifyNoInteractions(authTokenIssuer);
    }

    @Test
    @DisplayName("5. Tài khoản LOCKED không thể đăng nhập Admin -> ném 403 FORBIDDEN và KHÔNG cấp refresh token")
    void adminLogin_withLockedAccount_shouldThrowForbiddenAndNotIssueRefreshToken() {
        LoginRequest request = LoginRequest.builder()
                .account("locked_admin@example.invalid")
                .password("SamplePassword123!")
                .build();

        when(userRepository.findByEmailIgnoreCase("locked_admin@example.invalid")).thenReturn(Optional.of(lockedAdminUser));
        when(passwordEncoder.matches("SamplePassword123!", lockedAdminUser.getPasswordHash())).thenReturn(true);

        assertThatThrownBy(() -> adminAuthService.adminLogin(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verifyNoInteractions(authTokenIssuer);
    }

    @Test
    @DisplayName("6. Đăng nhập Admin sai mật khẩu -> ném 401 UNAUTHORIZED và KHÔNG cấp refresh token")
    void adminLogin_withWrongPassword_shouldThrowUnauthorizedAndNotIssueRefreshToken() {
        LoginRequest request = LoginRequest.builder()
                .account("admin@example.invalid")
                .password("WrongPass!")
                .build();

        when(userRepository.findByEmailIgnoreCase("admin@example.invalid")).thenReturn(Optional.of(adminUser));
        when(passwordEncoder.matches("WrongPass!", adminUser.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> adminAuthService.adminLogin(request))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.UNAUTHORIZED);

        verifyNoInteractions(authTokenIssuer);
    }
}
