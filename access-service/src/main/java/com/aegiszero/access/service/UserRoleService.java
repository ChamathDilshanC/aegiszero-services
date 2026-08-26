package com.aegiszero.access.service;

import com.aegiszero.access.entity.Role;
import com.aegiszero.access.entity.UserRole;
import com.aegiszero.access.event.AccessEventPublisher;
import com.aegiszero.access.repository.RoleRepository;
import com.aegiszero.access.repository.UserRoleRepository;
import com.aegiszero.common.exception.ConflictException;
import com.aegiszero.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserRoleService {

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final AccessEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<String> listRoleNamesForUser(UUID userId) {
        return userRoleRepository.findByUserIdWithRoleAndPermissions(userId).stream()
                .map(ur -> ur.getRole().getName())
                .toList();
    }

    @Transactional
    public void assignRole(UUID userId, String roleName, UUID actorId) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role '" + roleName + "' does not exist"));

        userRoleRepository.findByUserIdAndRoleId(userId, role.getId()).ifPresent(existing -> {
            throw new ConflictException("User already has role '" + roleName + "'");
        });

        userRoleRepository.save(UserRole.builder().userId(userId).role(role).build());
        eventPublisher.roleAssigned(userId, roleName, actorId);
    }

    /**
     * Same as {@link #assignRole}, but a no-op instead of a 409 if the user
     * already has the role. Used by system-initiated grants (e.g. an admin
     * access request being approved) where the caller has no good way to
     * check first and a double-fire (retry, double-click) must stay safe.
     */
    @Transactional
    public void assignRoleIfMissing(UUID userId, String roleName, UUID actorId) {
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role '" + roleName + "' does not exist"));

        if (userRoleRepository.findByUserIdAndRoleId(userId, role.getId()).isPresent()) {
            return;
        }

        userRoleRepository.save(UserRole.builder().userId(userId).role(role).build());
        eventPublisher.roleAssigned(userId, roleName, actorId);
    }

    @Transactional
    public void removeRole(UUID userId, UUID roleId, UUID actorId) {
        UserRole userRole = userRoleRepository.findByUserIdAndRoleId(userId, roleId)
                .orElseThrow(() -> new ResourceNotFoundException("User does not have this role"));
        String roleName = userRole.getRole().getName();
        userRoleRepository.delete(userRole);
        eventPublisher.roleRemoved(userId, roleName, actorId);
    }
}
