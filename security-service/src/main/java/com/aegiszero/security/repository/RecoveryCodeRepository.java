package com.aegiszero.security.repository;

import com.aegiszero.security.entity.RecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecoveryCodeRepository extends JpaRepository<RecoveryCode, UUID> {
    List<RecoveryCode> findByUserIdAndUsedFalse(UUID userId);
    Optional<RecoveryCode> findByUserIdAndCodeHash(UUID userId, String codeHash);
    void deleteByUserId(UUID userId);
}
