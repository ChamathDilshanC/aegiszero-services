package com.aegiszero.security.dto;

import com.aegiszero.security.entity.BlockedIp;

public record BlockedIpResponse(
        String id,
        String ipAddress,
        String reason,
        String createdAt
) {
    public static BlockedIpResponse from(BlockedIp blockedIp) {
        return new BlockedIpResponse(blockedIp.getId().toString(), blockedIp.getIpAddress(),
                blockedIp.getReason(), blockedIp.getCreatedAt().toString());
    }
}
