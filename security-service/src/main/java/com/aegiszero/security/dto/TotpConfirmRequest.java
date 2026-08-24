package com.aegiszero.security.dto;

import jakarta.validation.constraints.NotBlank;

public record TotpConfirmRequest(
        @NotBlank String code
) {
}
