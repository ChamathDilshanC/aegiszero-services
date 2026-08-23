package com.aegiszero.auth.service;

import com.aegiszero.auth.entity.Credential;
import com.aegiszero.auth.entity.PasswordResetToken;
import com.aegiszero.auth.event.AuthEventPublisher;
import com.aegiszero.auth.repository.CredentialRepository;
import com.aegiszero.auth.repository.PasswordResetTokenRepository;
import com.aegiszero.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private static final long RESET_TOKEN_TTL_MINUTES = 30;

    private final CredentialRepository credentialRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final AuthEventPublisher eventPublisher;

    @Transactional
    public void forgotPassword(String email) {
        credentialRepository.findByEmailIgnoreCase(email).ifPresent(credential -> {
            String rawToken = passwordService.generateOneTimeToken();
            PasswordResetToken token = PasswordResetToken.builder()
                    .userId(credential.getId())
                    .tokenHash(passwordService.hashToken(rawToken))
                    .expiresAt(Instant.now().plus(RESET_TOKEN_TTL_MINUTES, ChronoUnit.MINUTES))
                    .build();
            resetTokenRepository.save(token);

            eventPublisher.notify(credential.getEmail(), "PASSWORD_RESET", "Reset your AegisZero password",
                    Map.of("token", rawToken, "firstName", credential.getFirstName()));
        });
        // Deliberately do not reveal whether the email exists.
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String hash = passwordService.hashToken(rawToken);
        PasswordResetToken token = resetTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired reset token"));

        if (token.isUsed() || token.isExpired()) {
            throw new ResourceNotFoundException("Invalid or expired reset token");
        }

        Credential credential = credentialRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        credential.setPasswordHash(passwordService.hashPassword(newPassword));
        credential.setFailedLoginAttempts(0);
        credential.setLockedUntil(null);
        credentialRepository.save(credential);

        token.setUsed(true);
        resetTokenRepository.save(token);

        tokenService.revokeAllForUser(credential.getId());
        eventPublisher.passwordChanged(credential.getId());
    }
}
