package com.aegiszero.security.controller;

import com.aegiszero.security.dto.*;
import com.aegiszero.security.entity.MfaMethodType;
import com.aegiszero.security.service.MfaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/security/mfa")
@RequiredArgsConstructor
public class MfaController {

    private final MfaService mfaService;

    @GetMapping
    public ResponseEntity<MfaMethodsResponse> methods(Authentication authentication) {
        UUID userId = PrincipalSupport.currentUserId(authentication);
        return ResponseEntity.ok(mfaService.getMethods(userId));
    }

    @PostMapping("/enroll/totp")
    public ResponseEntity<TotpEnrollResponse> enrollTotp(Authentication authentication,
                                                           @RequestParam(required = false) String email) {
        UUID userId = PrincipalSupport.currentUserId(authentication);
        return ResponseEntity.ok(mfaService.enrollTotp(userId, email));
    }

    @PostMapping("/enroll/totp/confirm")
    public ResponseEntity<RecoveryCodesResponse> confirmTotp(Authentication authentication,
                                                               @Valid @RequestBody TotpConfirmRequest request) {
        UUID userId = PrincipalSupport.currentUserId(authentication);
        return ResponseEntity.ok(mfaService.confirmTotp(userId, request.code()));
    }

    @PostMapping("/enroll/email-otp")
    public ResponseEntity<Void> enableEmailOtp(Authentication authentication) {
        mfaService.enableEmailOtp(PrincipalSupport.currentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{method}")
    public ResponseEntity<Void> disable(@PathVariable String method, Authentication authentication) {
        UUID userId = PrincipalSupport.currentUserId(authentication);
        mfaService.disable(userId, MfaMethodType.valueOf(method.toUpperCase()));
        return ResponseEntity.noContent().build();
    }
}
