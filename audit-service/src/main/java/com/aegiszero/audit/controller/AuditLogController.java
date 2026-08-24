package com.aegiszero.audit.controller;

import com.aegiszero.audit.dto.AuditLogResponse;
import com.aegiszero.audit.dto.PageResponse;
import com.aegiszero.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/audit/logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public PageResponse<AuditLogResponse> search(@RequestParam(required = false) String eventType,
                                                  @RequestParam(required = false) String actorId,
                                                  @RequestParam(required = false) String targetId,
                                                  @RequestParam(required = false) Instant from,
                                                  @RequestParam(required = false) Instant to,
                                                  Pageable pageable) {
        return auditLogService.search(eventType, actorId, targetId, from, to, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AUDIT_READ')")
    public AuditLogResponse get(@PathVariable UUID id) {
        return auditLogService.get(id);
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('AUDIT_EXPORT')")
    public ResponseEntity<String> export(@RequestParam(required = false) String eventType,
                                          @RequestParam(required = false) String actorId,
                                          @RequestParam(required = false) String targetId,
                                          @RequestParam(required = false) Instant from,
                                          @RequestParam(required = false) Instant to) {
        List<AuditLogResponse> rows = auditLogService.searchAllForExport(eventType, actorId, targetId, from, to);

        StringBuilder csv = new StringBuilder("id,eventType,actorId,targetId,ipAddress,deviceId,occurredAt\n");
        for (AuditLogResponse row : rows) {
            csv.append(csvEscape(row.id())).append(',')
                    .append(csvEscape(row.eventType())).append(',')
                    .append(csvEscape(row.actorId())).append(',')
                    .append(csvEscape(row.targetId())).append(',')
                    .append(csvEscape(row.ipAddress())).append(',')
                    .append(csvEscape(row.deviceId())).append(',')
                    .append(csvEscape(row.occurredAt())).append('\n');
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
