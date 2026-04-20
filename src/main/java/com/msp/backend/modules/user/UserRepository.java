package com.msp.backend.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);
    Optional<User> findByDisplayNameIgnoreCase(String displayName);
    Optional<User> findByResetToken(String resetToken);
    List<User> findByDeletedAtIsNull();
    List<User> findByDeletedAtIsNullAndStatus(String status);
    List<User> findByLastLoginAtBeforeAndDeletedAtIsNullAndStatus(java.time.LocalDateTime cutoff, String status);
    boolean existsByEmail(String email);

    @Query("SELECT u.userId FROM User u WHERE u.userId LIKE :prefix%")
    List<String> findUserIdsByPrefix(@Param("prefix") String prefix);
}