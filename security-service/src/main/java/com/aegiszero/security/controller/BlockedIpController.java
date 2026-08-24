package com.aegiszero.security.controller;

import com.aegiszero.security.dto.BlockedIpRequest;
import com.aegiszero.security.dto.BlockedIpResponse;
import com.aegiszero.security.service.BlockedIpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/security/blocked-ips")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('SECURITY_MANAGE')")
public class BlockedIpController {

    private final BlockedIpService blockedIpService;

    @GetMapping
    public ResponseEntity<List<BlockedIpResponse>> list() {
        return ResponseEntity.ok(blockedIpService.list());
    }

    @PostMapping
    public ResponseEntity<BlockedIpResponse> block(@Valid @RequestBody BlockedIpRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(blockedIpService.block(request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> unblock(@PathVariable UUID id) {
        blockedIpService.unblock(id);
        return ResponseEntity.noContent().build();
    }
}
