package com.aegiszero.audit.service;

import com.aegiszero.common.event.AuditEvent;
import com.aegiszero.common.exception.ResourceNotFoundException;
import com.aegiszero.audit.dto.AuditLogResponse;
import com.aegiszero.audit.dto.PageResponse;
import com.aegiszero.audit.entity.AuditLog;
import com.aegiszero.audit.repository.AuditLogRepository;
import com.aegiszero.audit.repository.AuditLogSpecifications;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditLogService {

    private static final Logger log = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(AuditEvent event) {
        UUID id = UUID.fromString(event.eventId());
        if (auditLogRepository.existsById(id)) {
            return;
        }

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(event.metadata());
        } catch (Exception e) {
            log.warn("Failed to serialize audit metadata for event {}: {}", event.eventId(), e.getMessage());
            metadataJson = "{}";
        }

        AuditLog entity = AuditLog.builder()
                .id(id)
                .eventType(event.eventType())
                .actorId(event.actorId())
                .targetId(event.targetId())
                .ipAddress(event.ipAddress())
                .deviceId(event.deviceId())
                .correlationId(event.correlationId())
                .occurredAt(event.occurredAt() == null ? Instant.now() : event.occurredAt())
                .metadataJson(metadataJson)
                .build();

        auditLogRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(String eventType, String actorId, String targetId,
                                                  Instant from, Instant to, Pageable pageable) {
        var spec = AuditLogSpecifications.filter(eventType, actorId, targetId, from, to);
        return PageResponse.from(auditLogRepository.findAll(spec, pageable).map(AuditLogResponse::from));
    }

    @Transactional(readOnly = true)
    public AuditLogResponse get(UUID id) {
        return auditLogRepository.findById(id)
                .map(AuditLogResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Audit log entry not found"));
    }

    @Transactional(readOnly = true)
    public java.util.List<AuditLogResponse> searchAllForExport(String eventType, String actorId, String targetId,
                                                                 Instant from, Instant to) {
        var spec = AuditLogSpecifications.filter(eventType, actorId, targetId, from, to);
        return auditLogRepository.findAll(spec).stream().map(AuditLogResponse::from).toList();
    }
}
