package com.msp.backend.modules.auth;


import com.msp.backend.modules.auth.dto.LoginRequest;
import com.msp.backend.modules.auth.dto.AuthResponse;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    // Get current logged-in user info
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("displayName", user.getDisplayName());
        userInfo.put("role", user.getRole());
        userInfo.put("phoneNumber", user.getPhoneNumber());
        userInfo.put("status", user.getStatus());
        userInfo.put("merchantId", user.getMerchantId());
        userInfo.put("isMfaEnabled", user.isMfaEnabled());
        return userInfo;
    }

    // MFA verification endpoint
    @PostMapping("/mfa/verify")
    public ResponseEntity<Map<String, Object>> verifyMfa(@RequestBody Map<String, String> request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otpCode = request.get("code");
        if (otpCode == null || otpCode.length() != 6) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Invalid OTP code. Must be 6 digits."
            ));
        }

        // Verify the OTP against the user's secret key
        if (user.getSecretKey() == null || user.getSecretKey().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "MFA is not configured for this account. Please contact administrator."
            ));
        }

        // TOTP verification placeholder — integrate with Google Authenticator / TOTP library
        // For now, we validate the secret key exists and return a meaningful response
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "MFA verification service is being configured. Please contact administrator.");
        return ResponseEntity.ok(response);
    }
}