package com.aegiszero.auth.repository;

import com.aegiszero.auth.entity.AdminAccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AdminAccessRequestRepository extends JpaRepository<AdminAccessRequest, UUID> {
}
