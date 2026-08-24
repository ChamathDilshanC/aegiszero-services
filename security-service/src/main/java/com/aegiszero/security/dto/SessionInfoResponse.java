package com.aegiszero.security.dto;

public record SessionInfoResponse(
        String sessionId,
        String deviceId,
        String ipAddress,
        String userAgent,
        String createdAt,
        String lastActivityAt
) {
}
