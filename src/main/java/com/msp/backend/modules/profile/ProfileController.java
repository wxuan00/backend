package com.msp.backend.modules.profile;

import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Get own profile
    @GetMapping
    public ResponseEntity<User> getProfile() {
        User user = getCurrentUser();
        // Don't return password
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    // Update own profile (name, phone, display name)
    @PutMapping
    public ResponseEntity<User> updateProfile(@RequestBody Map<String, String> updates) {
        User user = getCurrentUser();

        if (updates.containsKey("firstName")) user.setFirstName(updates.get("firstName"));
        if (updates.containsKey("lastName")) user.setLastName(updates.get("lastName"));
        if (updates.containsKey("displayName")) user.setDisplayName(updates.get("displayName"));
        if (updates.containsKey("phoneNumber")) user.setPhoneNumber(updates.get("phoneNumber"));

        User saved = userRepository.save(user);
        saved.setPassword(null);
        return ResponseEntity.ok(saved);
    }

    // Change own password
    @PutMapping("/password")
    public ResponseEntity<Map<String, String>> changePassword(@RequestBody Map<String, String> passwords) {
        User user = getCurrentUser();

        String currentPassword = passwords.get("currentPassword");
        String newPassword = passwords.get("newPassword");

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Current password is incorrect"));
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
