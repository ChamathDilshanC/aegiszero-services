package com.aegiszero.access.repository;

import com.aegiszero.access.entity.Role;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(String name);

    boolean existsByName(String name);

    @EntityGraph(attributePaths = "permissions")
    @Override
    List<Role> findAll();

    @EntityGraph(attributePaths = "permissions")
    Optional<Role> findWithPermissionsById(UUID id);
}
