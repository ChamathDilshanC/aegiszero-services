package com.aegiszero.user.repository;

import com.aegiszero.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    // `query` must never be null here (see UserService.search) - PostgreSQL's
    // JDBC driver can't infer a bind parameter's type from "? IS NULL" alone
    // ("could not determine data type of parameter $1"), which a null :q used
    // to trigger on every page load with no search term. Always binding a
    // real string (blank = match everything, via LIKE '%%') sidesteps that
    // entirely instead of adding an explicit cast.
    @Query("SELECT u FROM User u WHERE LOWER(u.email) LIKE LOWER(CONCAT('%', :q, '%')) " +
           "OR LOWER(u.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<User> search(@Param("q") String query, Pageable pageable);
}
