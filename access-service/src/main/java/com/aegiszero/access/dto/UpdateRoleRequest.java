package com.aegiszero.access.dto;

import java.util.List;

public record UpdateRoleRequest(
        String description,
        List<String> permissions
) {
}
