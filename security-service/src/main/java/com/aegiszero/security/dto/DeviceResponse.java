package com.aegiszero.security.dto;

import com.aegiszero.security.entity.Device;

public record DeviceResponse(
        String id,
        String deviceName,
        boolean trusted,
        boolean blocked,
        String lastIp,
        String lastSeenAt,
        String createdAt
) {
    public static DeviceResponse from(Device device) {
        return new DeviceResponse(
                device.getId().toString(),
                device.getDeviceName(),
                device.isTrusted(),
                device.isBlocked(),
                device.getLastIp(),
                device.getLastSeenAt() == null ? null : device.getLastSeenAt().toString(),
                device.getCreatedAt().toString()
        );
    }
}
