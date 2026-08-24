package com.aegiszero.audit.event;

import com.aegiszero.audit.service.AuditLogService;
import com.aegiszero.common.event.AuditEvent;
import com.aegiszero.common.event.EventTopics;
import com.aegiszero.common.messaging.EventSubscription;
import org.springframework.stereotype.Component;

@Component
public class AuditEventListener implements EventSubscription<AuditEvent> {

    private final AuditLogService auditLogService;

    public AuditEventListener(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Override
    public String topic() {
        return EventTopics.AUDIT_EVENTS;
    }

    @Override
    public Class<AuditEvent> payloadType() {
        return AuditEvent.class;
    }

    @Override
    public void handle(AuditEvent event) {
        auditLogService.record(event);
    }
}
