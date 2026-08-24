package com.aegiszero.security.event;

import com.aegiszero.common.event.*;
import com.aegiszero.common.messaging.EventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SecurityEventPublisher {

    private final EventPublisher eventPublisher;

    public SecurityEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void riskCalculated(String userId, int score, String decision, List<String> reasons, String ip) {
        eventPublisher.publish(EventTopics.SECURITY_RISK_CALCULATED,
                new RiskCalculatedEvent(userId, score, decision, reasons, ip, Instant.now()));
        if ("BLOCK".equals(decision) || "REQUIRE_ADDITIONAL_VERIFICATION".equals(decision)) {
            eventPublisher.publish(EventTopics.SECURITY_ALERT_TRIGGERED,
                    new SecurityAlertEvent(userId, "HIGH_RISK_LOGIN",
                            "Login flagged as " + decision + " (score " + score + ")",
                            "BLOCK".equals(decision) ? "CRITICAL" : "HIGH", Instant.now()));
        }
    }

    public void deviceRegistered(String userId, String deviceId, String deviceName, boolean newDevice, String ip) {
        eventPublisher.publish(EventTopics.DEVICE_REGISTERED,
                new DeviceRegisteredEvent(userId, deviceId, deviceName, newDevice, ip, Instant.now()));
        if (newDevice) {
            audit("DEVICE_REGISTERED", userId, userId, ip, deviceId, Map.of());
        }
    }

    public void deviceBlocked(String userId, String deviceId, UUID actorId) {
        eventPublisher.publish(EventTopics.DEVICE_BLOCKED,
                new DeviceBlockedEvent(userId, deviceId, Instant.now()));
        audit("DEVICE_BLOCKED", actorId == null ? userId : actorId.toString(), userId, null, deviceId, Map.of());
    }

    public void sessionCreated(String userId, String sessionId, String deviceId, String ip) {
        eventPublisher.publish(EventTopics.SESSION_CREATED,
                new SessionCreatedEvent(userId, sessionId, deviceId, ip, Instant.now()));
    }

    public void sessionRevoked(String userId, String sessionId, String reason) {
        eventPublisher.publish(EventTopics.SESSION_REVOKED,
                new SessionRevokedEvent(userId, sessionId, reason, Instant.now()));
        audit("SESSION_TERMINATED", userId, userId, null, null, Map.of("sessionId", sessionId, "reason", reason));
    }

    public void mfaEnabled(String userId, String method) {
        eventPublisher.publish(EventTopics.SECURITY_ALERT_TRIGGERED,
                new SecurityAlertEvent(userId, "MFA_ENABLED", method + " enabled", "INFO", Instant.now()));
        audit("MFA_ENABLED", userId, userId, null, null, Map.of("method", method));
    }

    public void mfaDisabled(String userId, String method) {
        audit("MFA_DISABLED", userId, userId, null, null, Map.of("method", method));
    }

    public void notify(String recipientEmail, String type, String subject, Map<String, Object> templateData) {
        eventPublisher.publish(EventTopics.NOTIFICATION_EVENTS,
                new NotificationEvent(recipientEmail, type, subject, templateData, Instant.now()));
    }

    private void audit(String eventType, String actorId, String targetId, String ip, String deviceId, Map<String, Object> metadata) {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID().toString(), eventType, actorId, targetId, ip, deviceId,
                org.slf4j.MDC.get("correlationId"), Instant.now(), metadata
        );
        eventPublisher.publish(EventTopics.AUDIT_EVENTS, event);
    }
}
