package com.aegiszero.access.service;

import com.aegiszero.access.dto.AuthorizationResponse;
import com.aegiszero.access.entity.Permission;
import com.aegiszero.access.entity.Role;
import com.aegiszero.access.entity.UserRole;
import com.aegiszero.access.repository.RoleRepository;
import com.aegiszero.access.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthorizationQueryService {

    private static final Logger log = LoggerFactory.getLogger(AuthorizationQueryService.class);
    public static final String DEFAULT_ROLE = "USER";

    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;

    @Transactional
    public AuthorizationResponse getAuthorization(UUID userId) {
        List<UserRole> userRoles = userRoleRepository.findByUserIdWithRoleAndPermissions(userId);

        if (userRoles.isEmpty()) {
            // Defends against the race between auth-service registering a user
            // and the event listener that normally assigns the default role.
            assignDefaultRoleIfMissing(userId);
            userRoles = userRoleRepository.findByUserIdWithRoleAndPermissions(userId);
        }

        Set<String> roles = new TreeSet<>();
        Set<String> permissions = new TreeSet<>();
        for (UserRole userRole : userRoles) {
            Role role = userRole.getRole();
            roles.add(role.getName());
            for (Permission permission : role.getPermissions()) {
                permissions.add(permission.getName());
            }
        }

        return new AuthorizationResponse(List.copyOf(roles), List.copyOf(permissions));
    }

    @Transactional
    public void assignDefaultRoleIfMissing(UUID userId) {
        if (userRoleRepository.existsByUserId(userId)) {
            return;
        }
        roleRepository.findByName(DEFAULT_ROLE).ifPresentOrElse(
                role -> userRoleRepository.save(UserRole.builder().userId(userId).role(role).build()),
                () -> log.warn("Default role '{}' is not seeded; cannot auto-assign to user {}", DEFAULT_ROLE, userId)
        );
    }
}
