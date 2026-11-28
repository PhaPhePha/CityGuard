package com.cityguard.backend.service.impl;

import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.dto.auth.FirebaseLoginRequest;
import com.cityguard.backend.dto.auth.LoginRequest;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserFirebaseAccountRepository userFirebaseAccountRepository;
    @Mock
    private UserAuthProviderRepository userAuthProviderRepository;
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

    @Test
    void login_withNullPasswordHash_throwsUnauthorized() {
        User oauthUser = User.builder()
                .id(10L)
                .username("oauth_user")
                .email("oauth@example.com")
                .passwordHash(null)
                .displayName("OAuth User")
                .role("USER")
                .status("ACTIVE")
                .build();

        when(userRepository.findByEmailIgnoreCase("oauth@example.com")).thenReturn(Optional.of(oauthUser));

        LoginRequest request = LoginRequest.builder()
                .account("oauth@example.com")
                .password("anyPassword")
                .build();

        AppException ex = assertThrows(AppException.class, () -> authService.login(request));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
        verify(passwordEncoder, never()).matches(anyString(), any());
    }

    @Test
    void loginWithFirebase_newUser_createsAllThreeEntities() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("fb-uid-001");
        when(token.getEmail()).thenReturn("social.user@example.com");
        when(token.getName()).thenReturn("Social User");
        when(token.getPicture()).thenReturn("https://example.com/avatar.png");
        when(token.getClaims()).thenReturn(Map.of(
                "firebase", Map.of(
                        "sign_in_provider", "google.com",
                        "identities", Map.of("google.com", List.of("google-sub-001"))
                )
        ));

        when(userFirebaseAccountRepository.findByFirebaseUid("fb-uid-001")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("social.user@example.com")).thenReturn(false);
        when(userAuthProviderRepository.existsByProviderAndProviderUid("GOOGLE", "google-sub-001")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(101L);
            return u;
        });
        when(userFirebaseAccountRepository.save(any(UserFirebaseAccount.class))).thenAnswer(inv -> {
            UserFirebaseAccount fa = inv.getArgument(0);
            fa.setId(201L);
            return fa;
        });
        when(userAuthProviderRepository.save(any(UserAuthProvider.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse expectedResponse = AuthResponse.builder()
                .accessToken("access-jwt")
                .refreshToken("refresh-raw")
                .expiresIn(900000L)
                .userInfo(AuthResponse.UserInfo.builder().id(101L).build())
                .build();
        when(authTokenIssuer.issueAuthResponse(any(User.class))).thenReturn(expectedResponse);

        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = Mockito.mockStatic(FirebaseAuth.class)) {
            FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("valid-id-token")).thenReturn(token);

            FirebaseLoginRequest request = FirebaseLoginRequest.builder()
                    .idToken("valid-id-token")
                    .provider("GOOGLE")
                    .build();

            AuthResponse response = authService.loginWithFirebase(request);
            assertEquals("access-jwt", response.getAccessToken());
            assertEquals(101L, response.getUserInfo().getId());
        }

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertNull(savedUser.getPasswordHash());
        assertEquals("USER", savedUser.getRole());
        assertEquals("ACTIVE", savedUser.getStatus());
        assertTrue(savedUser.getUsername().matches("^[A-Za-z0-9_]{3,50}$"));

        ArgumentCaptor<UserFirebaseAccount> faCaptor = ArgumentCaptor.forClass(UserFirebaseAccount.class);
        verify(userFirebaseAccountRepository).save(faCaptor.capture());
        assertEquals("fb-uid-001", faCaptor.getValue().getFirebaseUid());
        assertEquals(101L, faCaptor.getValue().getUser().getId());

        ArgumentCaptor<UserAuthProvider> providerCaptor = ArgumentCaptor.forClass(UserAuthProvider.class);
        verify(userAuthProviderRepository).save(providerCaptor.capture());
        assertEquals("GOOGLE", providerCaptor.getValue().getProvider());
        assertEquals("google-sub-001", providerCaptor.getValue().getProviderUid());
        assertEquals(101L, providerCaptor.getValue().getUser().getId());
        assertNotNull(providerCaptor.getValue().getFirebaseAccount());
    }

    @Test
    void loginWithFirebase_lockedUser_throwsForbidden() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("fb-locked-001");
        when(token.getClaims()).thenReturn(Map.of(
                "firebase", Map.of(
                        "sign_in_provider", "google.com",
                        "identities", Map.of("google.com", List.of("google-locked-001"))
                )
        ));

        User lockedUser = User.builder()
                .id(55L)
                .username("locked_user")
                .status("LOCKED")
                .role("USER")
                .displayName("Locked")
                .build();
        UserFirebaseAccount fa = UserFirebaseAccount.builder()
                .id(77L)
                .user(lockedUser)
                .firebaseUid("fb-locked-001")
                .build();

        when(userFirebaseAccountRepository.findByFirebaseUid("fb-locked-001")).thenReturn(Optional.of(fa));

        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = Mockito.mockStatic(FirebaseAuth.class)) {
            FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("locked-token")).thenReturn(token);

            FirebaseLoginRequest request = FirebaseLoginRequest.builder()
                    .idToken("locked-token")
                    .provider("GOOGLE")
                    .build();

            AppException ex = assertThrows(AppException.class, () -> authService.loginWithFirebase(request));
            assertEquals(ErrorCode.FORBIDDEN, ex.getErrorCode());
        }
    }

    @Test
    void loginWithFirebase_existingEmailLocalAccount_throwsConflict() throws Exception {
        FirebaseToken token = mock(FirebaseToken.class);
        when(token.getUid()).thenReturn("fb-new-uid");
        when(token.getEmail()).thenReturn("local@example.com");
        when(token.getClaims()).thenReturn(Map.of(
                "firebase", Map.of(
                        "sign_in_provider", "facebook.com",
                        "identities", Map.of("facebook.com", List.of("fb-sub-999"))
                )
        ));

        when(userFirebaseAccountRepository.findByFirebaseUid("fb-new-uid")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("local@example.com")).thenReturn(true);

        try (MockedStatic<FirebaseAuth> firebaseAuthStatic = Mockito.mockStatic(FirebaseAuth.class)) {
            FirebaseAuth firebaseAuth = mock(FirebaseAuth.class);
            firebaseAuthStatic.when(FirebaseAuth::getInstance).thenReturn(firebaseAuth);
            when(firebaseAuth.verifyIdToken("dup-email-token")).thenReturn(token);

            FirebaseLoginRequest request = FirebaseLoginRequest.builder()
                    .idToken("dup-email-token")
                    .provider("FACEBOOK")
                    .build();

            AppException ex = assertThrows(AppException.class, () -> authService.loginWithFirebase(request));
            assertEquals(ErrorCode.RESOURCE_CONFLICT, ex.getErrorCode());
        }
    }
}
