package com.msp.backend.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByDisplayNameIgnoreCase(String displayName);
    List<User> findByDeletedAtIsNull();
    List<User> findByDeletedAtIsNullAndStatus(String status);
    List<User> findByLastLoginAtBeforeAndDeletedAtIsNullAndStatus(java.time.LocalDateTime cutoff, String status);
    boolean existsByEmail(String email);
}