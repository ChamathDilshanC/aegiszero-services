package com.aegiszero.auth.client.dto;

public record RiskEvaluationRequest(
        String userId,
        String ipAddress,
        String userAgent,
        String deviceFingerprint,
        String deviceName
) {
}
