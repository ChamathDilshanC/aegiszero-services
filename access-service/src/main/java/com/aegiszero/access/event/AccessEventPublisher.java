package com.aegiszero.access.event;

import com.aegiszero.common.event.AuditEvent;
import com.aegiszero.common.event.EventTopics;
import com.aegiszero.common.event.RoleChangedEvent;
import com.aegiszero.common.messaging.EventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class AccessEventPublisher {

    private final EventPublisher eventPublisher;

    public AccessEventPublisher(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public void roleAssigned(UUID userId, String roleName, UUID actorId) {
        eventPublisher.publish(EventTopics.USER_ROLE_CHANGED,
                new RoleChangedEvent(userId.toString(), roleName, "ASSIGNED", Instant.now()));
        audit("ROLE_ASSIGNED", actorId, userId, Map.of("role", roleName));
    }

    public void roleRemoved(UUID userId, String roleName, UUID actorId) {
        eventPublisher.publish(EventTopics.USER_ROLE_CHANGED,
                new RoleChangedEvent(userId.toString(), roleName, "REMOVED", Instant.now()));
        audit("ROLE_REMOVED", actorId, userId, Map.of("role", roleName));
    }

    private void audit(String eventType, UUID actorId, UUID targetId, Map<String, Object> metadata) {
        AuditEvent event = new AuditEvent(
                UUID.randomUUID().toString(),
                eventType,
                actorId == null ? "system" : actorId.toString(),
                targetId.toString(),
                null,
                null,
                org.slf4j.MDC.get("correlationId"),
                Instant.now(),
                metadata
        );
        eventPublisher.publish(EventTopics.AUDIT_EVENTS, event);
    }
}
