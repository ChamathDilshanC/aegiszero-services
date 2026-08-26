package com.aegiszero.auth.service;

import com.aegiszero.auth.client.AccessServiceClient;
import com.aegiszero.auth.entity.AdminAccessRequest;
import com.aegiszero.auth.entity.AdminAccessRequestStatus;
import com.aegiszero.auth.event.AuthEventPublisher;
import com.aegiszero.auth.repository.AdminAccessRequestRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Decides an admin access request from the one-click link an owner gets by
 * email (see {@link RegistrationService#requestAdminAccess}). Deliberately
 * not JSON — the caller here is a human's browser following an email link,
 * not the SPA, so {@link com.aegiszero.auth.controller.AdminAccessRequestController}
 * renders the outcome as a small HTML page instead.
 */
@Service
@RequiredArgsConstructor
public class AdminAccessRequestService {

    private static final Logger log = LoggerFactory.getLogger(AdminAccessRequestService.class);
    private static final String GRANTED_ROLE = "ADMIN";

    private final AdminAccessRequestRepository repository;
    private final PasswordService passwordService;
    private final AccessServiceClient accessServiceClient;
    private final AuthEventPublisher eventPublisher;

    public enum Outcome {
        APPROVED, REJECTED, ALREADY_DECIDED, EXPIRED, INVALID_TOKEN, NOT_FOUND, GRANT_FAILED
    }

    public record Decision(Outcome outcome, AdminAccessRequest request) {
    }

    @Transactional
    public Decision approve(UUID requestId, String rawToken) {
        Optional<AdminAccessRequest> found = repository.findById(requestId);
        if (found.isEmpty()) {
            return new Decision(Outcome.NOT_FOUND, null);
        }
        AdminAccessRequest request = found.get();

        Decision guard = guardPending(request, request.getApproveTokenHash(), rawToken);
        if (guard != null) {
            return guard;
        }

        try {
            accessServiceClient.assignRole(request.getUserId(), GRANTED_ROLE);
        } catch (Exception ex) {
            log.error("Failed to grant {} role to user {} for admin access request {}: {}",
                    GRANTED_ROLE, request.getUserId(), requestId, ex.getMessage());
            return new Decision(Outcome.GRANT_FAILED, request);
        }

        request.setStatus(AdminAccessRequestStatus.APPROVED);
        request.setDecidedAt(Instant.now());
        repository.save(request);

        eventPublisher.notify(request.getEmail(), "ADMIN_ACCESS_APPROVED", "Your AegisZero admin access was approved",
                Map.of("firstName", request.getFirstName()));

        return new Decision(Outcome.APPROVED, request);
    }

    @Transactional
    public Decision reject(UUID requestId, String rawToken) {
        Optional<AdminAccessRequest> found = repository.findById(requestId);
        if (found.isEmpty()) {
            return new Decision(Outcome.NOT_FOUND, null);
        }
        AdminAccessRequest request = found.get();

        Decision guard = guardPending(request, request.getRejectTokenHash(), rawToken);
        if (guard != null) {
            return guard;
        }

        request.setStatus(AdminAccessRequestStatus.REJECTED);
        request.setDecidedAt(Instant.now());
        repository.save(request);

        eventPublisher.notify(request.getEmail(), "ADMIN_ACCESS_REJECTED", "Your AegisZero admin access request was declined",
                Map.of("firstName", request.getFirstName()));

        return new Decision(Outcome.REJECTED, request);
    }

    private Decision guardPending(AdminAccessRequest request, String expectedTokenHash, String rawToken) {
        if (request.getStatus() != AdminAccessRequestStatus.PENDING) {
            return new Decision(Outcome.ALREADY_DECIDED, request);
        }
        if (request.isExpired()) {
            return new Decision(Outcome.EXPIRED, request);
        }
        if (rawToken == null || !expectedTokenHash.equals(passwordService.hashToken(rawToken))) {
            return new Decision(Outcome.INVALID_TOKEN, request);
        }
        return null;
    }
}
