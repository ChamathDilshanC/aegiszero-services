package com.aegiszero.auth.service;

import com.aegiszero.auth.client.AccessServiceClient;
import com.aegiszero.auth.client.dto.AuthorizationResponse;
import com.aegiszero.auth.dto.TokenResponse;
import com.aegiszero.auth.entity.RefreshToken;
import com.aegiszero.auth.repository.RefreshTokenRepository;
import com.aegiszero.common.exception.InvalidCredentialsException;
import com.aegiszero.common.security.jwt.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AccessServiceClient accessServiceClient;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        PasswordService passwordService = new PasswordService(org.springframework.security.crypto.argon2.Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        tokenService = new TokenService(jwtService, refreshTokenRepository, passwordService, accessServiceClient);

        lenient().when(jwtService.generateAccessToken(anyString(), anyString(), any(), any())).thenReturn("access-token");
        lenient().when(jwtService.generateOpaqueToken()).thenReturn(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        lenient().when(jwtService.getAccessTokenTtlSeconds()).thenReturn(900L);
        lenient().when(jwtService.getRefreshTokenTtlDays()).thenReturn(14L);

        // Mimic JPA assigning a generated id on save(), which the mocked repository can't do on its own.
        lenient().when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken token = invocation.getArgument(0);
            if (token.getId() == null) {
                token.setId(UUID.randomUUID());
            }
            return token;
        });
    }

    @Test
    void issueTokenPair_returnsBearerTokenWithBothAccessAndRefreshTokens() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();

        TokenResponse response = tokenService.issueTokenPair(userId, sessionId, List.of("USER"), List.of("PROFILE_READ"));

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(900L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void rotate_withValidToken_revokesOldAndIssuesNew() {
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .sessionId(sessionId)
                .tokenHash("irrelevant-because-mocked-lookup")
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));
        when(accessServiceClient.getAuthorization(userId)).thenReturn(new AuthorizationResponse(List.of("USER"), List.of("PROFILE_READ")));

        TokenResponse response = tokenService.rotate("some-raw-refresh-token");

        assertThat(response.accessToken()).isEqualTo("access-token");
        assertThat(existing.isRevoked()).isTrue();
        assertThat(existing.getReplacedBy()).isNotNull();
        verify(refreshTokenRepository, never()).revokeAllForSession(any());
    }

    @Test
    void rotate_withAlreadyRevokedToken_revokesWholeSessionAndThrows() {
        UUID sessionId = UUID.randomUUID();
        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .sessionId(sessionId)
                .tokenHash("irrelevant")
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .revoked(true)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tokenService.rotate("reused-token"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("reuse detected");

        verify(refreshTokenRepository).revokeAllForSession(sessionId);
        verify(accessServiceClient, never()).getAuthorization(any());
    }

    @Test
    void rotate_withExpiredToken_throwsWithoutRevokingSession() {
        RefreshToken existing = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .sessionId(UUID.randomUUID())
                .tokenHash("irrelevant")
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> tokenService.rotate("expired-token"))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void rotate_withUnknownToken_throwsInvalidCredentials() {
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tokenService.rotate("never-issued"))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void revokeAllForSession_delegatesToRepository() {
        UUID sessionId = UUID.randomUUID();
        tokenService.revokeAllForSession(sessionId);
        verify(refreshTokenRepository).revokeAllForSession(sessionId);
    }
}
