package com.aegiszero.access.controller;

import com.aegiszero.access.dto.AuthorizationResponse;
import com.aegiszero.access.service.AuthorizationQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Called service-to-service (never through the gateway) so that auth-service
 * can embed fresh roles/permissions into a JWT at login and refresh time.
 */
@RestController
@RequestMapping("/api/access/internal")
@RequiredArgsConstructor
public class InternalAuthorizationController {

    private final AuthorizationQueryService authorizationQueryService;

    @GetMapping("/users/{userId}/authorization")
    public AuthorizationResponse getAuthorization(@PathVariable UUID userId) {
        return authorizationQueryService.getAuthorization(userId);
    }
}
