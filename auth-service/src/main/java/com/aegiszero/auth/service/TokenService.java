package com.aegiszero.auth.service;

import com.aegiszero.auth.client.AccessServiceClient;
import com.aegiszero.auth.client.dto.AuthorizationResponse;
import com.aegiszero.auth.dto.TokenResponse;
import com.aegiszero.auth.entity.RefreshToken;
import com.aegiszero.auth.repository.RefreshTokenRepository;
import com.aegiszero.common.exception.InvalidCredentialsException;
import com.aegiszero.common.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordService passwordService;
    private final AccessServiceClient accessServiceClient;

    @Transactional
    public TokenResponse issueTokenPair(UUID userId, UUID sessionId, List<String> roles, List<String> permissions) {
        String accessToken = jwtService.generateAccessToken(userId.toString(), sessionId.toString(), roles, permissions);
        String rawRefreshToken = createRefreshToken(userId, sessionId);
        return TokenResponse.bearer(accessToken, rawRefreshToken, jwtService.getAccessTokenTtlSeconds());
    }

    private String createRefreshToken(UUID userId, UUID sessionId) {
        String rawToken = jwtService.generateOpaqueToken();
        RefreshToken entity = RefreshToken.builder()
                .userId(userId)
                .sessionId(sessionId)
                .tokenHash(passwordService.hashToken(rawToken))
                .expiresAt(Instant.now().plus(jwtService.getRefreshTokenTtlDays(), ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Rotates a refresh token. If a token that was already revoked is presented
     * again, that is a strong signal of theft/replay, so every active token for
     * the session is revoked defensively. The refresh token itself is opaque, so
     * the owning user is resolved here before fetching fresh roles/permissions.
     */
    @Transactional
    public TokenResponse rotate(String rawRefreshToken) {
        String hash = passwordService.hashToken(rawRefreshToken);
        RefreshToken existing = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new InvalidCredentialsException("Invalid refresh token"));

        if (existing.isRevoked()) {
            refreshTokenRepository.revokeAllForSession(existing.getSessionId());
            throw new InvalidCredentialsException("Refresh token reuse detected; session revoked");
        }

        if (existing.isExpired()) {
            throw new InvalidCredentialsException("Refresh token expired");
        }

        existing.setRevoked(true);

        AuthorizationResponse authorization = accessServiceClient.getAuthorization(existing.getUserId());
        List<String> roles = authorization.roles();
        List<String> permissions = authorization.permissions();

        String accessToken = jwtService.generateAccessToken(
                existing.getUserId().toString(), existing.getSessionId().toString(), roles, permissions);
        String newRawToken = jwtService.generateOpaqueToken();

        RefreshToken next = RefreshToken.builder()
                .userId(existing.getUserId())
                .sessionId(existing.getSessionId())
                .tokenHash(passwordService.hashToken(newRawToken))
                .expiresAt(Instant.now().plus(jwtService.getRefreshTokenTtlDays(), ChronoUnit.DAYS))
                .build();

        refreshTokenRepository.save(next);
        existing.setReplacedBy(next.getId());
        refreshTokenRepository.save(existing);

        return TokenResponse.bearer(accessToken, newRawToken, jwtService.getAccessTokenTtlSeconds());
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        String hash = passwordService.hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllForUser(userId);
    }

    @Transactional
    public void revokeAllForSession(UUID sessionId) {
        refreshTokenRepository.revokeAllForSession(sessionId);
    }
}
