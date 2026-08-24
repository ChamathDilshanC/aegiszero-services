package com.aegiszero.access.repository;

import com.aegiszero.access.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepository extends JpaRepository<UserRole, UUID> {

    @Query("SELECT ur FROM UserRole ur JOIN FETCH ur.role r LEFT JOIN FETCH r.permissions WHERE ur.userId = :userId")
    List<UserRole> findByUserIdWithRoleAndPermissions(@Param("userId") UUID userId);

    boolean existsByUserId(UUID userId);

    Optional<UserRole> findByUserIdAndRoleId(UUID userId, UUID roleId);

    List<UserRole> findByUserId(UUID userId);
}
