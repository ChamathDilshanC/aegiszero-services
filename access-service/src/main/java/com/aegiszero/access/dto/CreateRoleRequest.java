package com.aegiszero.access.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateRoleRequest(
        @NotBlank String name,
        String description,
        List<String> permissions
) {
}
