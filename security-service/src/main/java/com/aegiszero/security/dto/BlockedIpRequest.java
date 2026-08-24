package com.aegiszero.security.dto;

import jakarta.validation.constraints.NotBlank;

public record BlockedIpRequest(
        @NotBlank String ipAddress,
        String reason
) {
}
