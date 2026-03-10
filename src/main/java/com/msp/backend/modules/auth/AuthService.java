package com.msp.backend.modules.auth;

import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.modules.auth.dto.LoginRequest;
import com.msp.backend.modules.auth.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getDeletedAt() != null) {
            throw new RuntimeException("Account not found");
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new RuntimeException("Account is not active");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Populate role from UserRoles junction table
        userService.populateRole(user);

        if (user.getRole() == null || user.getRole().isBlank()) {
            throw new RuntimeException("User has no role assigned");
        }

        String jwtToken = jwtService.generateToken(user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(jwtToken)
                .role(user.getRole())
                .build();
    }
}