package com.aegiszero.auth.repository;

import com.aegiszero.auth.entity.Credential;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface CredentialRepository extends JpaRepository<Credential, UUID> {
    Optional<Credential> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
