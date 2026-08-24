package com.aegiszero.audit.repository;

import com.aegiszero.audit.entity.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class AuditLogSpecifications {

    private AuditLogSpecifications() {
    }

    public static Specification<AuditLog> filter(String eventType, String actorId, String targetId,
                                                   Instant from, Instant to) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (eventType != null && !eventType.isBlank()) {
                predicates = cb.and(predicates, cb.equal(root.get("eventType"), eventType));
            }
            if (actorId != null && !actorId.isBlank()) {
                predicates = cb.and(predicates, cb.equal(root.get("actorId"), actorId));
            }
            if (targetId != null && !targetId.isBlank()) {
                predicates = cb.and(predicates, cb.equal(root.get("targetId"), targetId));
            }
            if (from != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
            }
            if (to != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("occurredAt"), to));
            }
            return predicates;
        };
    }
}
