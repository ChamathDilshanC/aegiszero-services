package com.aegiszero.access.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(
        @NotBlank String roleName
) {
}
