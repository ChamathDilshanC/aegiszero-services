package com.aegiszero.auth.repository;

import com.aegiszero.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findByUserIdAndRevokedFalse(UUID userId);

    List<RefreshToken> findBySessionId(UUID sessionId);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.userId = :userId and r.revoked = false")
    void revokeAllForUser(@Param("userId") UUID userId);

    @Modifying
    @Query("update RefreshToken r set r.revoked = true where r.sessionId = :sessionId and r.revoked = false")
    void revokeAllForSession(@Param("sessionId") UUID sessionId);
}
