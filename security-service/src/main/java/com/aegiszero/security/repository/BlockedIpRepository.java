package com.aegiszero.security.repository;

import com.aegiszero.security.entity.BlockedIp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BlockedIpRepository extends JpaRepository<BlockedIp, UUID> {
    boolean existsByIpAddress(String ipAddress);
    Optional<BlockedIp> findByIpAddress(String ipAddress);
}
