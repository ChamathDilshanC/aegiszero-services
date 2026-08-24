package com.aegiszero.audit.dto;

import com.aegiszero.audit.entity.AuditLog;

public record AuditLogResponse(
        String id,
        String eventType,
        String actorId,
        String targetId,
        String ipAddress,
        String deviceId,
        String correlationId,
        String occurredAt,
        String metadata
) {
    public static AuditLogResponse from(AuditLog log) {
        return new AuditLogResponse(
                log.getId().toString(),
                log.getEventType(),
                log.getActorId(),
                log.getTargetId(),
                log.getIpAddress(),
                log.getDeviceId(),
                log.getCorrelationId(),
                log.getOccurredAt().toString(),
                log.getMetadataJson()
        );
    }
}
