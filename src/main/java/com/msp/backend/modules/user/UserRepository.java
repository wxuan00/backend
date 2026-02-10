package com.msp.backend.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // 1. For Login (Finds user by email)
    Optional<User> findByEmail(String email);

    // 2. For Dashboard (Only finds users who have NOT been soft-deleted)
    List<User> findByDeletedAtIsNull();

    boolean existsByEmail(String email);
}