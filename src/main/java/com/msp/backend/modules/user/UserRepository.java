package com.msp.backend.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring automatically writes the SQL: "SELECT * FROM users WHERE email = ?"
    Optional<User> findByEmail(String email);

    // Check if email exists (useful for registration)
    boolean existsByEmail(String email);
}