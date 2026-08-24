package com.aegiszero.auth.client.dto;

public record SessionCreateRequest(
        String userId,
        String deviceId,
        String ipAddress,
        String userAgent
) {
}
