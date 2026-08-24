package com.aegiszero.security.controller;

import com.aegiszero.common.exception.UnauthorizedException;
import com.aegiszero.security.dto.SessionInfoResponse;
import com.aegiszero.security.service.SessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/security/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionService sessionService;

    @GetMapping
    public ResponseEntity<List<SessionInfoResponse>> list(Authentication authentication) {
        UUID userId = PrincipalSupport.currentUserId(authentication);
        return ResponseEntity.ok(sessionService.listForUser(userId.toString()));
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<Void> revoke(@PathVariable String sessionId, Authentication authentication) {
        UUID userId = PrincipalSupport.currentUserId(authentication);
        if (!sessionService.belongsToUser(sessionId, userId.toString())) {
            throw new UnauthorizedException("This session does not belong to you");
        }
        sessionService.revoke(sessionId, "USER_REVOKED");
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> revokeAll(Authentication authentication) {
        UUID userId = PrincipalSupport.currentUserId(authentication);
        sessionService.revokeAllForUser(userId.toString(), "LOGOUT_ALL");
        return ResponseEntity.noContent().build();
    }
}
