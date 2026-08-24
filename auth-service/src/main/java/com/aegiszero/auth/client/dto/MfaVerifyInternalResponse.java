package com.aegiszero.auth.client.dto;

public record MfaVerifyInternalResponse(
        boolean verified,
        String userId,
        String deviceId,
        String ipAddress
) {
}
