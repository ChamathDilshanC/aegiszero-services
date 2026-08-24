package com.aegiszero.security.service;

import com.aegiszero.common.exception.ResourceNotFoundException;
import com.aegiszero.security.entity.Device;
import com.aegiszero.security.event.SecurityEventPublisher;
import com.aegiszero.security.repository.DeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceService {

    public record Resolution(Device device, boolean isNew) {
    }

    private final DeviceRepository deviceRepository;
    private final SecurityEventPublisher eventPublisher;

    @Transactional
    public Resolution resolveForLogin(UUID userId, String fingerprint, String deviceName, String ipAddress) {
        String effectiveFingerprint = (fingerprint == null || fingerprint.isBlank())
                ? "unknown-" + UUID.randomUUID()
                : fingerprint;

        var existing = deviceRepository.findByUserIdAndFingerprint(userId, effectiveFingerprint);
        if (existing.isPresent()) {
            Device device = existing.get();
            device.setLastIp(ipAddress);
            device.setLastSeenAt(Instant.now());
            deviceRepository.save(device);
            return new Resolution(device, false);
        }

        Device device = Device.builder()
                .userId(userId)
                .fingerprint(effectiveFingerprint)
                .deviceName(deviceName == null ? "Unknown device" : deviceName)
                .lastIp(ipAddress)
                .lastSeenAt(Instant.now())
                .build();
        device = deviceRepository.save(device);
        eventPublisher.deviceRegistered(userId.toString(), device.getId().toString(), device.getDeviceName(), true, ipAddress);
        return new Resolution(device, true);
    }

    @Transactional(readOnly = true)
    public List<Device> listForUser(UUID userId) {
        return deviceRepository.findByUserId(userId);
    }

    @Transactional
    public void trust(UUID userId, UUID deviceId) {
        Device device = ownedDevice(userId, deviceId);
        device.setTrusted(true);
        device.setBlocked(false);
        deviceRepository.save(device);
    }

    @Transactional
    public void block(UUID userId, UUID deviceId, UUID actorId) {
        Device device = ownedDevice(userId, deviceId);
        device.setBlocked(true);
        device.setTrusted(false);
        deviceRepository.save(device);
        eventPublisher.deviceBlocked(userId.toString(), deviceId.toString(), actorId);
    }

    @Transactional
    public void forget(UUID userId, UUID deviceId) {
        Device device = ownedDevice(userId, deviceId);
        deviceRepository.delete(device);
    }

    private Device ownedDevice(UUID userId, UUID deviceId) {
        return deviceRepository.findByIdAndUserId(deviceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
    }
}
