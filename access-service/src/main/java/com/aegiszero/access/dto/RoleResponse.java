package com.aegiszero.access.dto;

import com.aegiszero.access.entity.Role;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record RoleResponse(
        String id,
        String name,
        String description,
        boolean systemRole,
        List<String> permissions,
        Instant createdAt
) {
    public static RoleResponse from(Role role) {
        Set<String> permissionNames = role.getPermissions().stream()
                .map(com.aegiszero.access.entity.Permission::getName)
                .collect(Collectors.toSet());
        return new RoleResponse(
                role.getId().toString(),
                role.getName(),
                role.getDescription(),
                role.isSystemRole(),
                permissionNames.stream().sorted().toList(),
                role.getCreatedAt()
        );
    }
}
