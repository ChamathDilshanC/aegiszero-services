package com.aegiszero.access.dto;

import jakarta.validation.constraints.NotBlank;

public record AccessCheckRequest(
        @NotBlank String userId,
        @NotBlank String permission
) {
}
