package com.aegiszero.security.controller;

import com.aegiszero.security.dto.DeviceResponse;
import com.aegiszero.security.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/security/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public ResponseEntity<List<DeviceResponse>> list(Authentication authentication) {
        UUID userId = PrincipalSupport.currentUserId(authentication);
        return ResponseEntity.ok(deviceService.listForUser(userId).stream().map(DeviceResponse::from).toList());
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<Void> forget(@PathVariable UUID deviceId, Authentication authentication) {
        deviceService.forget(PrincipalSupport.currentUserId(authentication), deviceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{deviceId}/trust")
    public ResponseEntity<Void> trust(@PathVariable UUID deviceId, Authentication authentication) {
        deviceService.trust(PrincipalSupport.currentUserId(authentication), deviceId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{deviceId}/block")
    public ResponseEntity<Void> block(@PathVariable UUID deviceId, Authentication authentication) {
        UUID userId = PrincipalSupport.currentUserId(authentication);
        deviceService.block(userId, deviceId, userId);
        return ResponseEntity.noContent().build();
    }
}
