package com.msp.backend.modules.user;

import com.msp.backend.modules.role.Role;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.util.AuditHelper;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setCreatedAt(java.time.LocalDateTime.now());
        user.setCreatedBy(AuditHelper.currentUser());
        user.setLastModifiedBy(AuditHelper.currentUser());
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
        List<UserRole> roles = userRoleRepository.findByUserId(user.getUserId());
        if (!roles.isEmpty()) {
            Long roleId = roles.get(0).getRoleId();
            roleRepository.findById(roleId).ifPresent(role -> user.setRole(role.getRoleName()));
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
        user.setLastModifiedAt(java.time.LocalDateTime.now());
        userRepository.save(user);
    }
}
