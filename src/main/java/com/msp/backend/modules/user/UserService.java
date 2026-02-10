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

        if ("admin@msp.com".equalsIgnoreCase(user.getEmail())) {
            throw new RuntimeException("CRITICAL: You cannot delete the Super Admin!");
        } //to be improved with roles

        // Mark as deleted now
        user.setDeletedAt(java.time.LocalDateTime.now());

        // Save the update
        userRepository.save(user);
    }
}