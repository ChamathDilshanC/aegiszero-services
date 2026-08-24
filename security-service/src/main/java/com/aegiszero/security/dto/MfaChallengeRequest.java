package com.aegiszero.security.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaChallengeRequest(
        @NotBlank String userId,
        @NotBlank String method,
        String email
) {
}
