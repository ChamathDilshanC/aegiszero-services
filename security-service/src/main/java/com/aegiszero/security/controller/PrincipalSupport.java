package com.aegiszero.security.controller;

import com.aegiszero.common.exception.UnauthorizedException;
import com.aegiszero.common.security.jwt.AegisPrincipal;
import org.springframework.security.core.Authentication;

import java.util.UUID;

final class PrincipalSupport {

    private PrincipalSupport() {
    }

    static UUID currentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof AegisPrincipal principal) {
            return UUID.fromString(principal.userId());
        }
        throw new UnauthorizedException("Authentication required");
    }
}
