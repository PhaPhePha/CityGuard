package com.cityguard.backend.security;

import com.cityguard.backend.dto.auth.AuthResponse;
import com.cityguard.backend.entity.RefreshToken;
import com.cityguard.backend.entity.User;
import com.cityguard.backend.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthTokenIssuerTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthTokenIssuer authTokenIssuer;

    @Test
    void issueAuthResponse_shouldGenerateTokensPersistHashAndBuildResponse() {
        User user = User.builder()
                .id(10L)
                .username("demo_citizen")
                .email("citizen@example.invalid")
                .displayName("Demo Citizen")
                .role("USER")
                .status("ACTIVE")
                .build();

        String expectedAccessToken = "jwt.access.token";
        String expectedRawRefreshToken = "raw-refresh-token-64-chars";
        String expectedHash = "a".repeat(64);
        long accessExpMs = 3600000L;
        long refreshExpMs = 604800000L;

        when(jwtService.generateAccessToken(user)).thenReturn(expectedAccessToken);
        when(jwtService.generateRawRefreshToken()).thenReturn(expectedRawRefreshToken);
        when(jwtService.hashToken(expectedRawRefreshToken)).thenReturn(expectedHash);
        when(jwtService.getAccessExpirationMs()).thenReturn(accessExpMs);
        when(jwtService.getRefreshExpirationMs()).thenReturn(refreshExpMs);
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OffsetDateTime beforeCall = OffsetDateTime.now();
        AuthResponse response = authTokenIssuer.issueAuthResponse(user);
        OffsetDateTime afterCall = OffsetDateTime.now();

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();

        assertThat(savedToken.getUser()).isSameAs(user);
        assertThat(savedToken.getTokenHash()).isEqualTo(expectedHash);
        assertThat(savedToken.getRevokedAt()).isNull();
        assertThat(savedToken.getExpiresAt())
                .isAfterOrEqualTo(beforeCall.plusSeconds(refreshExpMs / 1000 - 1))
                .isBeforeOrEqualTo(afterCall.plusSeconds(refreshExpMs / 1000 + 1));

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(expectedAccessToken);
        assertThat(response.getRefreshToken()).isEqualTo(expectedRawRefreshToken);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(accessExpMs);
        assertThat(response.getUserInfo()).isNotNull();
        assertThat(response.getUserInfo().getId()).isEqualTo(10L);
        assertThat(response.getUserInfo().getUsername()).isEqualTo("demo_citizen");
        assertThat(response.getUserInfo().getEmail()).isEqualTo("citizen@example.invalid");
        assertThat(response.getUserInfo().getDisplayName()).isEqualTo("Demo Citizen");
        assertThat(response.getUserInfo().getRole()).isEqualTo("USER");
    }
}
