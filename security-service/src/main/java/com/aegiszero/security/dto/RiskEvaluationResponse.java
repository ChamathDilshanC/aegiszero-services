package com.aegiszero.security.dto;

import java.util.List;

public record RiskEvaluationResponse(
        int riskScore,
        String decision,
        List<String> reasons,
        String deviceId,
        boolean newDevice
) {
}
