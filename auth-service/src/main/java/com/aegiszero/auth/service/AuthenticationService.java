package com.aegiszero.auth.service;

import com.aegiszero.auth.client.AccessServiceClient;
import com.aegiszero.auth.client.SecurityServiceClient;
import com.aegiszero.auth.client.dto.*;
import com.aegiszero.auth.dto.LoginRequest;
import com.aegiszero.auth.dto.LoginResponse;
import com.aegiszero.auth.dto.MfaVerifyRequest;
import com.aegiszero.auth.dto.TokenResponse;
import com.aegiszero.auth.entity.AccountStatus;
import com.aegiszero.auth.entity.Credential;
import com.aegiszero.auth.event.AuthEventPublisher;
import com.aegiszero.auth.repository.CredentialRepository;
import com.aegiszero.common.exception.AccountLockedException;
import com.aegiszero.common.exception.InvalidCredentialsException;
import com.aegiszero.common.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_MINUTES = 15;

    private final CredentialRepository credentialRepository;
    private final PasswordService passwordService;
    private final TokenService tokenService;
    private final AccessServiceClient accessServiceClient;
    private final SecurityServiceClient securityServiceClient;
    private final PendingLoginStore pendingLoginStore;
    private final AuthEventPublisher eventPublisher;

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        Credential credential = credentialRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(() -> {
                    eventPublisher.loginFailed(null, request.email(), ipAddress, "UNKNOWN_EMAIL");
                    return new InvalidCredentialsException("Invalid email or password");
                });

        eventPublisher.loginAttempted(credential.getId().toString(), credential.getEmail(),
                ipAddress, userAgent, request.deviceFingerprint());
                
        enforceAccountStatus(credential);

        if (!passwordService.matches(request.password(), credential.getPasswordHash())) {
            registerFailedAttempt(credential);
            eventPublisher.loginFailed(credential.getId().toString(), credential.getEmail(), ipAddress, "BAD_PASSWORD");
            throw new InvalidCredentialsException("Invalid email or password");
        }

        clearFailedAttempts(credential);

        RiskEvaluationResponse risk = securityServiceClient.evaluateRisk(new RiskEvaluationRequest(
                credential.getId().toString(), ipAddress, userAgent, request.deviceFingerprint(), request.deviceName()));

        if ("BLOCK".equals(risk.decision())) {
            eventPublisher.loginFailed(credential.getId().toString(), credential.getEmail(), ipAddress, "RISK_BLOCKED");
            throw new AccountLockedException("Login blocked: this attempt was flagged as high risk");
        }

        boolean mfaRequired = "REQUIRE_MFA".equals(risk.decision())
                || "REQUIRE_ADDITIONAL_VERIFICATION".equals(risk.decision());

        if (mfaRequired) {
            MfaMethodsResponse mfaMethods = securityServiceClient.getMfaMethods(credential.getId().toString());
            String method = (mfaMethods.enabled() && !mfaMethods.methods().isEmpty())
                    ? mfaMethods.methods().get(0)
                    : "EMAIL_OTP";

            MfaChallengeResponse challenge = securityServiceClient.createChallenge(
                    new MfaChallengeRequest(credential.getId().toString(), method, credential.getEmail()));

            pendingLoginStore.put(challenge.challengeId(), credential.getId(), credential.getEmail(),
                    ipAddress, userAgent, risk.deviceId());

            return LoginResponse.mfaRequired(challenge.challengeId(), method);
        }

        return completeLogin(credential, ipAddress, userAgent, risk.deviceId());
    }

    @Transactional
    public LoginResponse verifyMfa(MfaVerifyRequest request) {
        MfaVerifyInternalResponse result = securityServiceClient.verifyChallenge(
                new MfaVerifyInternalRequest(request.challengeId(), request.code()));

        if (!result.verified()) {
            throw new InvalidCredentialsException("Invalid or expired verification code");
        }

        PendingLoginStore.PendingLogin pending = pendingLoginStore.consume(request.challengeId())
                .orElseThrow(() -> new InvalidCredentialsException("This login challenge has expired; please sign in again"));

        Credential credential = credentialRepository.findById(pending.credentialId())
                .orElseThrow(() -> new InvalidCredentialsException("Account not found"));

        return completeLogin(credential, pending.ipAddress(), pending.userAgent(), pending.deviceId());
    }

    private LoginResponse completeLogin(Credential credential, String ipAddress, String userAgent, String deviceId) {
        AuthorizationResponse authorization = accessServiceClient.getAuthorization(credential.getId());

        SessionResponse session = securityServiceClient.createSession(
                new SessionCreateRequest(credential.getId().toString(), deviceId, ipAddress, userAgent));

        TokenResponse tokens = tokenService.issueTokenPair(
                credential.getId(), UUID.fromString(session.sessionId()),
                authorization.roles(), authorization.permissions());

        eventPublisher.loginSucceeded(credential.getId().toString(), session.sessionId(), deviceId, ipAddress);

        return LoginResponse.success(tokens);
    }

    @Transactional
    public TokenResponse refresh(String rawRefreshToken) {
        return tokenService.rotate(rawRefreshToken);
    }

    @Transactional
    public void logout(String rawRefreshToken, String sessionId) {
        tokenService.revoke(rawRefreshToken);
        if (sessionId != null) {
            securityServiceClient.revokeSession(sessionId);
        }
    }

    private void enforceAccountStatus(Credential credential) {
        if (credential.getAccountStatus() == AccountStatus.DISABLED) {
            throw new UnauthorizedException("This account has been disabled");
        }
        if (credential.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            throw new UnauthorizedException("Please verify your email before signing in");
        }
        if (credential.getAccountStatus() == AccountStatus.LOCKED) {
            if (credential.getLockedUntil() != null && Instant.now().isBefore(credential.getLockedUntil())) {
                throw new AccountLockedException("Account temporarily locked due to repeated failed sign-in attempts");
            }
            credential.setAccountStatus(AccountStatus.ACTIVE);
            credential.setFailedLoginAttempts(0);
            credential.setLockedUntil(null);
            credentialRepository.save(credential);
        }
    }

    private void registerFailedAttempt(Credential credential) {
        int attempts = credential.getFailedLoginAttempts() + 1;
        credential.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            credential.setAccountStatus(AccountStatus.LOCKED);
            credential.setLockedUntil(Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
        }
        credentialRepository.save(credential);
    }

    private void clearFailedAttempts(Credential credential) {
        if (credential.getFailedLoginAttempts() > 0) {
            credential.setFailedLoginAttempts(0);
            credentialRepository.save(credential);
        }
    }
}
