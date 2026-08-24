package com.aegiszero.access.service;

import com.aegiszero.access.dto.CreatePermissionRequest;
import com.aegiszero.access.dto.PermissionResponse;
import com.aegiszero.access.entity.Permission;
import com.aegiszero.access.repository.PermissionRepository;
import com.aegiszero.common.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<PermissionResponse> list() {
        return permissionRepository.findAll().stream().map(PermissionResponse::from).toList();
    }

    @Transactional
    public PermissionResponse create(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.name())) {
            throw new ConflictException("Permission '" + request.name() + "' already exists");
        }
        Permission permission = Permission.builder()
                .name(request.name())
                .description(request.description())
                .build();
        return PermissionResponse.from(permissionRepository.save(permission));
    }
}
