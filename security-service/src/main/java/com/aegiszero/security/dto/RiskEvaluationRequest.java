package com.aegiszero.security.dto;

import jakarta.validation.constraints.NotBlank;

public record RiskEvaluationRequest(
        @NotBlank String userId,
        String ipAddress,
        String userAgent,
        String deviceFingerprint,
        String deviceName
) {
}
