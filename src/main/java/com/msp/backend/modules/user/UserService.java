package com.msp.backend.modules.user;

import com.msp.backend.modules.role.Role;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.util.AuditHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager entityManager;

    public List<User> getAllUsers() {
        List<User> users = userRepository.findByDeletedAtIsNull();
        users.forEach(this::populateRole);
        return users;
    }

    public User getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        populateRole(user);
        return user;
    }

    @Transactional
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        String roleName = user.getRole();
        if (roleName == null) roleName = "MERCHANT";
        if (user.getStatus() == null) user.setStatus("ACTIVE");

        String rawPassword = (user.getPassword() == null || user.getPassword().isBlank())
                ? "P@ssw0rd"
                : user.getPassword();
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setCreatedBy(AuditHelper.currentUser());
        user.setLastModifiedBy(AuditHelper.currentUser());
        user.setMustChangePassword(true); // force password change on first login
        user.setRole(null);
        User saved = userRepository.save(user);

        assignRole(saved.getUserId(), roleName, "SYSTEM");
        saved.setRole(roleName);
        return saved;
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        populateRole(user);

        if ("ADMIN".equals(user.getRole())) {
            long adminCount = userRepository.findByDeletedAtIsNull().stream()
                    .filter(u -> {
                        populateRole(u);
                        return "ADMIN".equals(u.getRole());
                    })
                    .count();
            if (adminCount <= 1) {
                throw new RuntimeException("CRITICAL: Cannot delete the last admin user!");
            }
        }

        user.setDeletedAt(java.time.LocalDateTime.now());
        user.setLastModifiedBy(AuditHelper.currentUser());
        user.setLastModifiedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public User updateUser(Long id, User updated) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Only overwrite if value is non-null AND non-blank (blank = "no change sent")
        if (updated.getFirstName() != null && !updated.getFirstName().isBlank())
            user.setFirstName(updated.getFirstName());
        if (updated.getLastName() != null && !updated.getLastName().isBlank())
            user.setLastName(updated.getLastName());
        if (updated.getDisplayName() != null)
            user.setDisplayName(updated.getDisplayName().isBlank() ? null : updated.getDisplayName());
        if (updated.getContactNumber() != null)
            user.setContactNumber(updated.getContactNumber().isBlank() ? null : updated.getContactNumber());
        if (updated.getStatus() != null && !updated.getStatus().isBlank())
            user.setStatus(updated.getStatus());

        user.setLastModifiedBy(AuditHelper.currentUser());
        user.setLastModifiedAt(java.time.LocalDateTime.now());
        // password is never updated through this method

        if (updated.getRole() != null) {
            userRoleRepository.deleteByUserId(user.getUserId());
            assignRole(user.getUserId(), updated.getRole(), AuditHelper.currentUser());
        }

        userRepository.saveAndFlush(user);
        entityManager.detach(user); // detach after flush so no dirty-check on response mutations
        populateRole(user);
        user.setPassword(null); // clear from response only
        return user;
    }

    public void populateRole(User user) {
        List<UserRole> userRoles = userRoleRepository.findByUserId(user.getUserId());
        if (userRoles.isEmpty()) return;

        // Prefer ADMIN or MERCHANT (the system role) over any custom role
        for (UserRole ur : userRoles) {
            roleRepository.findById(ur.getRoleId()).ifPresent(role -> {
                if ("ADMIN".equals(role.getRoleName()) || "MERCHANT".equals(role.getRoleName())) {
                    user.setRole(role.getRoleName());
                }
            });
        }
        // Fallback: if no system role found, use the first role available
        if (user.getRole() == null) {
            roleRepository.findById(userRoles.get(0).getRoleId())
                    .ifPresent(role -> user.setRole(role.getRoleName()));
        }
    }

    private void assignRole(Long userId, String roleName, String generatedBy) {
        Role role = roleRepository.findByRoleName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));
        UserRole userRole = new UserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getRoleId());
        userRole.setGeneratedBy(generatedBy);
        userRoleRepository.save(userRole);
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setLastModifiedBy(AuditHelper.currentUser());
        user.setLastModifiedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    /**
     * Runs daily at 02:00 AM. Auto-inactivates users who have never logged in AND
     * whose account was created more than 30 days ago, OR users whose last login
     * was more than 30 days ago.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void autoInactivateInactiveUsers() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
        List<User> candidates = userRepository.findByDeletedAtIsNullAndStatus("ACTIVE");
        for (User user : candidates) {
            boolean neverLoggedIn = user.getLastLoginAt() == null
                    && user.getCreatedAt() != null
                    && user.getCreatedAt().isBefore(cutoff);
            boolean inactiveTooLong = user.getLastLoginAt() != null
                    && user.getLastLoginAt().isBefore(cutoff);
            if (neverLoggedIn || inactiveTooLong) {
                user.setStatus("INACTIVE");
                user.setLastModifiedBy("SYSTEM");
                user.setLastModifiedAt(LocalDateTime.now());
                userRepository.save(user);
            }
        }
    }
}
