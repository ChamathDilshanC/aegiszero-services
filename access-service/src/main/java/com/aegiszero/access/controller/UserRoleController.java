package com.aegiszero.access.controller;

import com.aegiszero.access.dto.AssignRoleRequest;
import com.aegiszero.access.service.UserRoleService;
import com.aegiszero.common.security.jwt.AegisPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/access/users/{userId}/roles")
@RequiredArgsConstructor
public class UserRoleController {

    private final UserRoleService userRoleService;

    @GetMapping
    @PreAuthorize("hasAuthority('ROLE_READ')")
    public ResponseEntity<List<String>> list(@PathVariable UUID userId) {
        return ResponseEntity.ok(userRoleService.listRoleNamesForUser(userId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    public ResponseEntity<Void> assign(@PathVariable UUID userId, @Valid @RequestBody AssignRoleRequest request,
                                        Authentication authentication) {
        userRoleService.assignRole(userId, request.roleName(), actorId(authentication));
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize("hasAuthority('ROLE_ASSIGN')")
    public ResponseEntity<Void> remove(@PathVariable UUID userId, @PathVariable UUID roleId,
                                        Authentication authentication) {
        userRoleService.removeRole(userId, roleId, actorId(authentication));
        return ResponseEntity.noContent().build();
    }

    private UUID actorId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AegisPrincipal principal) {
            return UUID.fromString(principal.userId());
        }
        return null;
    }
}
