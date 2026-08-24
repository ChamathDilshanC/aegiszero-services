package com.aegiszero.access.dto;

import com.aegiszero.access.entity.Permission;

public record PermissionResponse(
        String id,
        String name,
        String description
) {
    public static PermissionResponse from(Permission permission) {
        return new PermissionResponse(permission.getId().toString(), permission.getName(), permission.getDescription());
    }
}
