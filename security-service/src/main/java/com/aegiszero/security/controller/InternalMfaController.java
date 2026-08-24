package com.aegiszero.security.controller;

import com.aegiszero.security.dto.*;
import com.aegiszero.security.service.MfaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/security/internal")
@RequiredArgsConstructor
public class InternalMfaController {

    private final MfaService mfaService;

    @GetMapping("/users/{userId}/mfa-methods")
    public MfaMethodsResponse getMethods(@PathVariable UUID userId) {
        return mfaService.getMethods(userId);
    }

    @PostMapping("/mfa/challenge")
    public MfaChallengeResponse createChallenge(@Valid @RequestBody MfaChallengeRequest request) {
        return mfaService.createChallenge(request);
    }

    @PostMapping("/mfa/verify")
    public MfaVerifyInternalResponse verifyChallenge(@Valid @RequestBody MfaVerifyInternalRequest request) {
        return mfaService.verifyChallenge(request);
    }
}
