package com.aegiszero.access.controller;

import com.aegiszero.access.dto.AssignRoleRequest;
import com.aegiszero.access.service.UserRoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Called service-to-service (never through the gateway), guarded by
 * {@link com.aegiszero.common.security.internal.InternalApiKeyFilter} the
 * same way as {@link InternalAuthorizationController}. auth-service uses
 * this to grant a role outside of any human's own JWT session — e.g.
 * completing an admin-access request that was approved by email link,
 * where there is no logged-in actor to carry {@code ROLE_ASSIGN}.
 */
@RestController
@RequestMapping("/api/access/internal")
@RequiredArgsConstructor
public class InternalUserRoleController {

    private final UserRoleService userRoleService;

    @PostMapping("/users/{userId}/roles")
    public ResponseEntity<Void> assignRoleIfMissing(@PathVariable UUID userId, @Valid @RequestBody AssignRoleRequest request) {
        userRoleService.assignRoleIfMissing(userId, request.roleName(), null);
        return ResponseEntity.noContent().build();
    }
}
