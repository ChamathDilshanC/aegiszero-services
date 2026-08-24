package com.aegiszero.security.service;

import com.aegiszero.common.exception.ResourceNotFoundException;
import com.aegiszero.security.dto.SessionInfoResponse;
import com.aegiszero.security.event.SecurityEventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SessionService {

    private static final String SESSION_KEY_PREFIX = "session:";
    private static final String USER_SESSIONS_KEY_PREFIX = "user:sessions:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityEventPublisher eventPublisher;
    private final long sessionTtlDays;

    public SessionService(RedisTemplate<String, Object> redisTemplate,
                           SecurityEventPublisher eventPublisher,
                           @Value("${aegiszero.jwt.refresh-token-ttl-days:14}") long sessionTtlDays) {
        this.redisTemplate = redisTemplate;
        this.eventPublisher = eventPublisher;
        this.sessionTtlDays = sessionTtlDays;
    }

    public String create(String userId, String deviceId, String ipAddress, String userAgent) {
        String sessionId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        SessionRecord record = new SessionRecord(userId, deviceId, ipAddress, userAgent, now, now);

        Duration ttl = Duration.ofDays(sessionTtlDays);
        redisTemplate.opsForValue().set(sessionKey(sessionId), record, ttl);
        redisTemplate.opsForSet().add(userSessionsKey(userId), sessionId);
        redisTemplate.expire(userSessionsKey(userId), ttl);

        eventPublisher.sessionCreated(userId, sessionId, deviceId, ipAddress);
        return sessionId;
    }

    public List<SessionInfoResponse> listForUser(String userId) {
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey(userId));
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }

        return sessionIds.stream()
                .map(Object::toString)
                .map(id -> {
                    SessionRecord record = (SessionRecord) redisTemplate.opsForValue().get(sessionKey(id));
                    if (record == null) {
                        redisTemplate.opsForSet().remove(userSessionsKey(userId), id);
                        return null;
                    }
                    return new SessionInfoResponse(id, record.getDeviceId(), record.getIpAddress(),
                            record.getUserAgent(), record.getCreatedAt().toString(), record.getLastActivityAt().toString());
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    public void revoke(String sessionId, String reason) {
        SessionRecord record = (SessionRecord) redisTemplate.opsForValue().get(sessionKey(sessionId));
        if (record == null) {
            throw new ResourceNotFoundException("Session not found");
        }
        redisTemplate.delete(sessionKey(sessionId));
        redisTemplate.opsForSet().remove(userSessionsKey(record.getUserId()), sessionId);
        eventPublisher.sessionRevoked(record.getUserId(), sessionId, reason);
    }

    /** Revokes a session on behalf of its owner without an ownership check (used by internal/admin callers). */
    public void revokeInternal(String sessionId, String reason) {
        SessionRecord record = (SessionRecord) redisTemplate.opsForValue().get(sessionKey(sessionId));
        if (record == null) {
            return;
        }
        redisTemplate.delete(sessionKey(sessionId));
        redisTemplate.opsForSet().remove(userSessionsKey(record.getUserId()), sessionId);
        eventPublisher.sessionRevoked(record.getUserId(), sessionId, reason);
    }

    public void revokeAllForUser(String userId, String reason) {
        Set<Object> sessionIds = redisTemplate.opsForSet().members(userSessionsKey(userId));
        if (sessionIds == null) {
            return;
        }
        for (Object sessionId : sessionIds) {
            redisTemplate.delete(sessionKey(sessionId.toString()));
            eventPublisher.sessionRevoked(userId, sessionId.toString(), reason);
        }
        redisTemplate.delete(userSessionsKey(userId));
    }

    public boolean belongsToUser(String sessionId, String userId) {
        SessionRecord record = (SessionRecord) redisTemplate.opsForValue().get(sessionKey(sessionId));
        return record != null && record.getUserId().equals(userId);
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String userSessionsKey(String userId) {
        return USER_SESSIONS_KEY_PREFIX + userId;
    }
}
