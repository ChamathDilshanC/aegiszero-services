package com.aegiszero.auth.event;

import com.aegiszero.common.event.*;
import com.aegiszero.common.messaging.EventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class AuthEventPublisher {

    private final EventPublisher eventPublisher;

    public AuthEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void userRegistered(UUID userId, String email, String firstName, String lastName) {
        eventPublisher.publish(EventTopics.USER_REGISTERED, userId.toString(),
                new UserRegisteredEvent(userId.toString(), email, firstName, lastName, Instant.now()));
        audit("USER_REGISTERED", userId.toString(), userId.toString(), null, Map.of("email", email));
    }

    public void loginAttempted(String userId, String email, String ip, String userAgent, String deviceFingerprint) {
        eventPublisher.publish(EventTopics.AUTH_LOGIN_ATTEMPTED,
                new LoginAttemptedEvent(userId, email, ip, userAgent, deviceFingerprint, Instant.now()));
    }

    public void loginSucceeded(String userId, String sessionId, String deviceId, String ip) {
        eventPublisher.publish(EventTopics.AUTH_LOGIN_SUCCEEDED,
                new LoginSucceededEvent(userId, sessionId, deviceId, ip, Instant.now()));
        audit("LOGIN_SUCCESS", userId, userId, ip, Map.of("sessionId", sessionId, "deviceId", String.valueOf(deviceId)));
    }

    public void loginFailed(String userId, String email, String ip, String reason) {
        eventPublisher.publish(EventTopics.AUTH_LOGIN_FAILED,
                new LoginFailedEvent(userId, email, ip, reason, Instant.now()));
        audit("LOGIN_FAILED", userId, userId, ip, Map.of("email", email, "reason", reason));
    }

    public void passwordChanged(UUID userId) {
        eventPublisher.publish(EventTopics.USER_PASSWORD_CHANGED,
                new PasswordChangedEvent(userId.toString(), Instant.now()));
        audit("PASSWORD_CHANGED", userId.toString(), userId.toString(), null, Map.of());
    }

    public void sessionRevoked(String userId, String sessionId, String reason) {
        audit("SESSION_TERMINATED", userId, userId, null, Map.of("sessionId", sessionId, "reason", reason));
    }

    public void notify(String recipientEmail, String type, String subject, Map<String, Object> templateData) {
        eventPublisher.publish(EventTopics.NOTIFICATION_EVENTS,
                new NotificationEvent(recipientEmail, type, subject, templateData, Instant.now()));
    }

    private void audit(String eventType, String actorId, String targetId, String ipAddress, Map<String, Object> metadata) {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID().toString(),
                eventType,
                actorId,
                targetId,
                ipAddress,
                null,
                org.slf4j.MDC.get("correlationId"),
                Instant.now(),
                metadata
        );
        eventPublisher.publish(EventTopics.AUDIT_EVENTS, event);
    }
}
