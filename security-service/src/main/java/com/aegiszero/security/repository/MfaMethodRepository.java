package com.aegiszero.security.repository;

import com.aegiszero.security.entity.MfaMethod;
import com.aegiszero.security.entity.MfaMethodType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MfaMethodRepository extends JpaRepository<MfaMethod, UUID> {
    List<MfaMethod> findByUserIdAndEnabledTrue(UUID userId);
    Optional<MfaMethod> findByUserIdAndType(UUID userId, MfaMethodType type);
}
