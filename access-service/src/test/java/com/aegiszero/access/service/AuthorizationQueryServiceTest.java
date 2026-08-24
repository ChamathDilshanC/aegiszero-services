package com.aegiszero.access.service;

import com.aegiszero.access.dto.AuthorizationResponse;
import com.aegiszero.access.entity.Permission;
import com.aegiszero.access.entity.Role;
import com.aegiszero.access.entity.UserRole;
import com.aegiszero.access.repository.RoleRepository;
import com.aegiszero.access.repository.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationQueryServiceTest {

    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private RoleRepository roleRepository;

    private AuthorizationQueryService service;

    @BeforeEach
    void setUp() {
        service = new AuthorizationQueryService(userRoleRepository, roleRepository);
    }

    private Role roleWithPermissions(String name, String... permissionNames) {
        Set<Permission> permissions = new java.util.HashSet<>();
        for (String p : permissionNames) {
            permissions.add(Permission.builder().id(UUID.randomUUID()).name(p).build());
        }
        return Role.builder().id(UUID.randomUUID()).name(name).permissions(permissions).build();
    }

    @Test
    void getAuthorization_aggregatesRolesAndPermissionsAcrossMultipleRoles() {
        UUID userId = UUID.randomUUID();
        Role admin = roleWithPermissions("ADMIN", "USER_READ", "USER_UPDATE");
        Role user = roleWithPermissions("USER", "PROFILE_READ", "USER_READ");

        UserRole userRoleAdmin = UserRole.builder().userId(userId).role(admin).build();
        UserRole userRoleUser = UserRole.builder().userId(userId).role(user).build();

        when(userRoleRepository.findByUserIdWithRoleAndPermissions(userId))
                .thenReturn(List.of(userRoleAdmin, userRoleUser));

        AuthorizationResponse response = service.getAuthorization(userId);

        assertThat(response.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
        assertThat(response.permissions()).containsExactlyInAnyOrder("USER_READ", "USER_UPDATE", "PROFILE_READ");
    }

    @Test
    void getAuthorization_withNoRoles_autoAssignsDefaultRoleThenRetries() {
        UUID userId = UUID.randomUUID();
        Role defaultRole = roleWithPermissions("USER", "PROFILE_READ");
        UserRole assigned = UserRole.builder().userId(userId).role(defaultRole).build();

        when(userRoleRepository.findByUserIdWithRoleAndPermissions(userId))
                .thenReturn(List.of())
                .thenReturn(List.of(assigned));
        when(userRoleRepository.existsByUserId(userId)).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(defaultRole));

        AuthorizationResponse response = service.getAuthorization(userId);

        assertThat(response.roles()).containsExactly("USER");
        assertThat(response.permissions()).containsExactly("PROFILE_READ");
        verify(userRoleRepository).save(any(UserRole.class));
    }

    @Test
    void assignDefaultRoleIfMissing_doesNothingWhenUserAlreadyHasARole() {
        UUID userId = UUID.randomUUID();
        when(userRoleRepository.existsByUserId(userId)).thenReturn(true);

        service.assignDefaultRoleIfMissing(userId);

        verify(roleRepository, never()).findByName(any());
        verify(userRoleRepository, never()).save(any());
    }

    @Test
    void assignDefaultRoleIfMissing_doesNotThrowWhenDefaultRoleIsNotSeeded() {
        UUID userId = UUID.randomUUID();
        when(userRoleRepository.existsByUserId(userId)).thenReturn(false);
        when(roleRepository.findByName("USER")).thenReturn(Optional.empty());

        service.assignDefaultRoleIfMissing(userId);

        verify(userRoleRepository, never()).save(any());
    }
}
