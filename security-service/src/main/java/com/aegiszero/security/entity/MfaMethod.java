package com.aegiszero.security.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mfa_methods", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaMethod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MfaMethodType type;

    /** Base32 TOTP shared secret. Unused for EMAIL_OTP. */
    @Column(name = "secret")
    private String secret;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
