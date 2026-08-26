package com.aegiszero.auth.service;

import com.aegiszero.auth.dto.RegisterRequest;
import com.aegiszero.auth.entity.AccountStatus;
import com.aegiszero.auth.entity.AdminAccessRequest;
import com.aegiszero.auth.entity.Credential;
import com.aegiszero.auth.entity.EmailVerificationToken;
import com.aegiszero.auth.event.AuthEventPublisher;
import com.aegiszero.auth.repository.AdminAccessRequestRepository;
import com.aegiszero.auth.repository.CredentialRepository;
import com.aegiszero.auth.repository.EmailVerificationTokenRepository;
import com.aegiszero.common.exception.ConflictException;
import com.aegiszero.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private static final long VERIFICATION_TOKEN_TTL_HOURS = 24;
    private static final long ADMIN_REQUEST_TTL_DAYS = 7;

    private final CredentialRepository credentialRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final AdminAccessRequestRepository adminAccessRequestRepository;
    private final PasswordService passwordService;
    private final AuthEventPublisher eventPublisher;

    @Value("${aegiszero.admin-request.notify-email:}")
    private String adminRequestNotifyEmail;

    @Value("${aegiszero.public-base-url:http://localhost:8081}")
    private String publicBaseUrl;

    @Transactional
    public void register(RegisterRequest request) {
        if (credentialRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflictException("An account with this email already exists");
        }

        Credential credential = Credential.builder()
                .email(request.email().toLowerCase())
                .passwordHash(passwordService.hashPassword(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .accountStatus(AccountStatus.PENDING_VERIFICATION)
                .build();
        credential = credentialRepository.save(credential);

        eventPublisher.userRegistered(credential.getId(), credential.getEmail(),
                credential.getFirstName(), credential.getLastName());

        String rawToken = issueVerificationToken(credential.getId());

        eventPublisher.notify(credential.getEmail(), "EMAIL_VERIFICATION", "Verify your AegisZero account",
                Map.of("token", rawToken, "firstName", credential.getFirstName()));

        if (request.isRequestingAdminAccess()) {
            requestAdminAccess(credential);
        }
    }

    private void requestAdminAccess(Credential credential) {
        if (adminRequestNotifyEmail == null || adminRequestNotifyEmail.isBlank()) {
            // Not configured on this deployment - skip rather than send nowhere.
            return;
        }

        String approveToken = passwordService.generateOneTimeToken();
        String rejectToken = passwordService.generateOneTimeToken();

        AdminAccessRequest accessRequest = AdminAccessRequest.builder()
                .userId(credential.getId())
                .email(credential.getEmail())
                .firstName(credential.getFirstName())
                .lastName(credential.getLastName())
                .approveTokenHash(passwordService.hashToken(approveToken))
                .rejectTokenHash(passwordService.hashToken(rejectToken))
                .expiresAt(Instant.now().plus(ADMIN_REQUEST_TTL_DAYS, ChronoUnit.DAYS))
                .build();
        accessRequest = adminAccessRequestRepository.save(accessRequest);

        String approveUrl = publicBaseUrl + "/api/auth/admin-requests/" + accessRequest.getId() + "/approve?token=" + approveToken;
        String rejectUrl = publicBaseUrl + "/api/auth/admin-requests/" + accessRequest.getId() + "/reject?token=" + rejectToken;

        eventPublisher.notify(adminRequestNotifyEmail, "ADMIN_ACCESS_REQUEST", "Admin access requested: " + credential.getEmail(),
                Map.of(
                        "firstName", credential.getFirstName(),
                        "lastName", credential.getLastName(),
                        "email", credential.getEmail(),
                        "approveUrl", approveUrl,
                        "rejectUrl", rejectUrl
                ));
    }

    private String issueVerificationToken(java.util.UUID userId) {
        String rawToken = passwordService.generateOneTimeToken();
        EmailVerificationToken token = EmailVerificationToken.builder()
                .userId(userId)
                .tokenHash(passwordService.hashToken(rawToken))
                .expiresAt(Instant.now().plus(VERIFICATION_TOKEN_TTL_HOURS, ChronoUnit.HOURS))
                .build();
        verificationTokenRepository.save(token);
        return rawToken;
    }

    @Transactional
    public void verifyEmail(String rawToken) {
        String hash = passwordService.hashToken(rawToken);
        EmailVerificationToken token = verificationTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired verification token"));

        if (token.isUsed() || token.isExpired()) {
            throw new ResourceNotFoundException("Invalid or expired verification token");
        }

        Credential credential = credentialRepository.findById(token.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        credential.setEmailVerified(true);
        if (credential.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            credential.setAccountStatus(AccountStatus.ACTIVE);
        }
        credentialRepository.save(credential);

        token.setUsed(true);
        verificationTokenRepository.save(token);
    }
}
