package com.aegiszero.security.dto;

import jakarta.validation.constraints.NotBlank;

public record SessionCreateRequest(
        @NotBlank String userId,
        String deviceId,
        String ipAddress,
        String userAgent
) {
}
