package com.msp.backend.modules.auth;

import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.modules.auth.dto.LoginRequest;
import com.msp.backend.modules.auth.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier();
        if (identifier == null || identifier.isBlank()) {
            throw new RuntimeException("Email or display name is required");
        }

        // Try email first, then display name
        User user = userRepository.findByEmail(identifier)
                .or(() -> userRepository.findByDisplayNameIgnoreCase(identifier))
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (user.getDeletedAt() != null) {
            throw new RuntimeException("Invalid credentials");
        }

        if ("INACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Account inactive. Please contact admin for assistance.");
        }

        if ("SUSPENDED".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Account suspended. Please contact admin for assistance.");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Account is not active. Please contact admin for assistance.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        userService.populateRole(user);

        if (user.getRole() == null || user.getRole().isBlank()) {
            throw new RuntimeException("User has no role assigned");
        }

        // Record last login time
        user.setLastLoginAt(java.time.LocalDateTime.now());
        userRepository.save(user);

        String jwtToken = jwtService.generateToken(user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(jwtToken)
                .role(user.getRole())
                .build();
    }
}