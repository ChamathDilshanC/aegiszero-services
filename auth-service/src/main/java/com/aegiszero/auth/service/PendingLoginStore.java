package com.aegiszero.auth.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the login context (who, from where) between an MFA challenge being
 * issued and the client completing it, keyed by challengeId. A single-instance,
 * in-memory store is sufficient for the MVP; a horizontally scaled deployment
 * would move this into Redis alongside session state.
 */
@Component
public class PendingLoginStore {

    private static final long TTL_MINUTES = 5;

    public record PendingLogin(UUID credentialId, String email, String ipAddress, String userAgent,
                                String deviceId, Instant createdAt) {
        boolean isExpired() {
            return Instant.now().isAfter(createdAt.plusSeconds(TTL_MINUTES * 60));
        }
    }

    private final Map<String, PendingLogin> store = new ConcurrentHashMap<>();

    public void put(String challengeId, UUID credentialId, String email, String ipAddress,
                     String userAgent, String deviceId) {
        store.put(challengeId, new PendingLogin(credentialId, email, ipAddress, userAgent, deviceId, Instant.now()));
    }

    public Optional<PendingLogin> consume(String challengeId) {
        PendingLogin pending = store.remove(challengeId);
        if (pending == null || pending.isExpired()) {
            return Optional.empty();
        }
        return Optional.of(pending);
    }
}
