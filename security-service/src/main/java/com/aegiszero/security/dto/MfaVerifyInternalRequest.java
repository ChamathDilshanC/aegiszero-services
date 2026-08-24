package com.aegiszero.security.dto;

import jakarta.validation.constraints.NotBlank;

public record MfaVerifyInternalRequest(
        @NotBlank String challengeId,
        @NotBlank String code
) {
}
