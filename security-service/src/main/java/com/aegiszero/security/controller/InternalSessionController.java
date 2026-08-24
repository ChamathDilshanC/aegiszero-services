package com.aegiszero.security.controller;

import com.aegiszero.security.dto.SessionCreateRequest;
import com.aegiszero.security.dto.SessionResponse;
import com.aegiszero.security.service.SessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/security/internal/sessions")
@RequiredArgsConstructor
public class InternalSessionController {

    private final SessionService sessionService;

    @PostMapping
    public SessionResponse create(@Valid @RequestBody SessionCreateRequest request) {
        String sessionId = sessionService.create(request.userId(), request.deviceId(),
                request.ipAddress(), request.userAgent());
        return new SessionResponse(sessionId);
    }

    @DeleteMapping("/{sessionId}")
    public void revoke(@PathVariable String sessionId) {
        sessionService.revokeInternal(sessionId, "LOGOUT");
    }
}
