package com.aegiszero.access.service;

import com.aegiszero.access.dto.CreateRoleRequest;
import com.aegiszero.access.dto.RoleResponse;
import com.aegiszero.access.dto.UpdateRoleRequest;
import com.aegiszero.access.entity.Permission;
import com.aegiszero.access.entity.Role;
import com.aegiszero.access.repository.PermissionRepository;
import com.aegiszero.access.repository.RoleRepository;
import com.aegiszero.common.exception.ConflictException;
import com.aegiszero.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<RoleResponse> list() {
        return roleRepository.findAll().stream().map(RoleResponse::from).toList();
    }

    @Transactional
    public RoleResponse create(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.name())) {
            throw new ConflictException("Role '" + request.name() + "' already exists");
        }
        Role role = Role.builder()
                .name(request.name())
                .description(request.description())
                .permissions(resolvePermissions(request.permissions()))
                .build();
        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse update(UUID roleId, UpdateRoleRequest request) {
        Role role = roleRepository.findWithPermissionsById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));

        if (request.description() != null) {
            role.setDescription(request.description());
        }
        if (request.permissions() != null) {
            role.setPermissions(resolvePermissions(request.permissions()));
        }
        return RoleResponse.from(roleRepository.save(role));
    }

    @Transactional
    public void delete(UUID roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        if (role.isSystemRole()) {
            throw new ConflictException("System roles cannot be deleted");
        }
        roleRepository.delete(role);
    }

    private Set<Permission> resolvePermissions(List<String> permissionNames) {
        if (permissionNames == null || permissionNames.isEmpty()) {
            return new HashSet<>();
        }
        Set<Permission> permissions = new HashSet<>();
        for (String name : permissionNames) {
            Permission permission = permissionRepository.findByName(name)
                    .orElseThrow(() -> new ResourceNotFoundException("Permission '" + name + "' does not exist"));
            permissions.add(permission);
        }
        return permissions;
    }
}
