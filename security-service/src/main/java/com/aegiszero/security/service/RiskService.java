package com.aegiszero.security.service;

import com.aegiszero.security.dto.RiskEvaluationRequest;
import com.aegiszero.security.dto.RiskEvaluationResponse;
import com.aegiszero.security.entity.Device;
import com.aegiszero.security.event.SecurityEventPublisher;
import com.aegiszero.security.repository.BlockedIpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rule-based risk scoring per architecture.md section 4.10. Signals that need
 * a GeoIP database (new country, impossible travel) are intentionally not
 * implemented in this MVP; repeated-failed-login tracking already lives in
 * auth-service's account lockout, so it is not duplicated here.
 */
@Service
@RequiredArgsConstructor
public class RiskService {

    private static final int SCORE_NEW_DEVICE = 25;
    private static final int SCORE_TRUSTED_DEVICE = -20;
    private static final int SCORE_BLOCKED_IP = 80;

    private static final int THRESHOLD_MEDIUM = 31;
    private static final int THRESHOLD_HIGH = 61;
    private static final int THRESHOLD_CRITICAL = 91;

    private final DeviceService deviceService;
    private final BlockedIpRepository blockedIpRepository;
    private final SecurityEventPublisher eventPublisher;

    @Transactional
    public RiskEvaluationResponse evaluate(RiskEvaluationRequest request) {
        UUID userId = UUID.fromString(request.userId());
        DeviceService.Resolution resolution = deviceService.resolveForLogin(
                userId, request.deviceFingerprint(), request.deviceName(), request.ipAddress());
        Device device = resolution.device();

        if (device.isBlocked()) {
            eventPublisher.riskCalculated(request.userId(), 100, "BLOCK", List.of("DEVICE_BLOCKED"), request.ipAddress());
            return new RiskEvaluationResponse(100, "BLOCK", List.of("DEVICE_BLOCKED"),
                    device.getId().toString(), resolution.isNew());
        }

        int score = 0;
        List<String> reasons = new ArrayList<>();

        if (resolution.isNew()) {
            score += SCORE_NEW_DEVICE;
            reasons.add("NEW_DEVICE");
        } else if (device.isTrusted()) {
            score += SCORE_TRUSTED_DEVICE;
            reasons.add("TRUSTED_DEVICE");
        }

        if (request.ipAddress() != null && blockedIpRepository.existsByIpAddress(request.ipAddress())) {
            score += SCORE_BLOCKED_IP;
            reasons.add("BLOCKED_IP");
        }

        score = Math.max(0, score);
        String decision = decisionFor(score);

        eventPublisher.riskCalculated(request.userId(), score, decision, reasons, request.ipAddress());

        return new RiskEvaluationResponse(score, decision, reasons, device.getId().toString(), resolution.isNew());
    }

    private String decisionFor(int score) {
        if (score >= THRESHOLD_CRITICAL) {
            return "BLOCK";
        }
        if (score >= THRESHOLD_HIGH) {
            return "REQUIRE_ADDITIONAL_VERIFICATION";
        }
        if (score >= THRESHOLD_MEDIUM) {
            return "REQUIRE_MFA";
        }
        return "ALLOW";
    }
}
