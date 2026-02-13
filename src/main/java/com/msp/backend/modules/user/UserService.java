package com.msp.backend.modules.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
@RequiredArgsConstructor // Lombok generates the constructor for dependency injection
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Get all users
    public List<User> getAllUsers() {
        return userRepository.findByDeletedAtIsNull();
    }

    // Get single user by ID
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Create a new user
    public User createUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("Email already exists!");
        }

        // Basic validation
        if (user.getRole() == null) user.setRole("USER");
        if (user.getStatus() == null) user.setStatus("ACTIVE");

        user.setPassword(passwordEncoder.encode(user.getPassword())); // Date is set by @PrePersist in entity
        user.setCreatedAt(java.time.LocalDateTime.now());
        // In the next step (Auth), we will encrypt this password!
        return userRepository.save(user);
    }

    public void deleteUser(Long id) {
        // Soft delete
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Protect admin users from being deleted
        if ("ADMIN".equals(user.getRole())) {
            // Count remaining admin users
            long adminCount = userRepository.findByDeletedAtIsNull().stream()
                    .filter(u -> "ADMIN".equals(u.getRole()))
                    .count();
            if (adminCount <= 1) {
                throw new RuntimeException("CRITICAL: Cannot delete the last admin user!");
            }
        }

        // Mark as deleted now
        user.setDeletedAt(java.time.LocalDateTime.now());

        // Save the update
        userRepository.save(user);
    }

    // Update an existing user
    public User updateUser(Long id, User updated) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (updated.getFirstName() != null) user.setFirstName(updated.getFirstName());
        if (updated.getLastName() != null) user.setLastName(updated.getLastName());
        if (updated.getDisplayName() != null) user.setDisplayName(updated.getDisplayName());
        if (updated.getPhoneNumber() != null) user.setPhoneNumber(updated.getPhoneNumber());
        if (updated.getRole() != null) user.setRole(updated.getRole());
        if (updated.getStatus() != null) user.setStatus(updated.getStatus());
        if (updated.getMerchantId() != null) user.setMerchantId(updated.getMerchantId());
        // Only update password if provided
        if (updated.getPassword() != null && !updated.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(updated.getPassword()));
        }

        User saved = userRepository.save(user);
        saved.setPassword(null); // Don't return password
        return saved;
    }
}