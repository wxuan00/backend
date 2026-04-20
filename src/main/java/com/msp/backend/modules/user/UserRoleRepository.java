package com.msp.backend.modules.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {
    List<UserRole> findByUserId(String userId);
    void deleteByUserId(String userId);
}
