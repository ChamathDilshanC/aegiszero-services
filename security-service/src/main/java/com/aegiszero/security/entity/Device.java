package com.aegiszero.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "devices", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "fingerprint"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String fingerprint;

    @Column(name = "device_name")
    private String deviceName;

    @Column(name = "trusted", nullable = false)
    @Builder.Default
    private boolean trusted = false;

    @Column(name = "blocked", nullable = false)
    @Builder.Default
    private boolean blocked = false;

    @Column(name = "last_ip")
    private String lastIp;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
