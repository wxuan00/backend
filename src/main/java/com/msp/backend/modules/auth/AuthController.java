package com.msp.backend.modules.auth;


import com.msp.backend.modules.auth.dto.LoginRequest;
import com.msp.backend.modules.auth.dto.AuthResponse;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
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
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantRepository merchantRepository;
    private final TotpService totpService;

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody LoginRequest request) {
        AuthResponse authResponse = authService.login(request);
        
        // Check if user has MFA enabled
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Map<String, Object> response = new HashMap<>();
        response.put("token", authResponse.getToken());
        response.put("role", authResponse.getRole());
        
        if (user.isMfaEnabled() && user.getSecretKey() != null && !user.getSecretKey().isBlank()) {
            // MFA is required
            response.put("mfaRequired", true);
        } else {
            // No MFA required
            response.put("mfaRequired", false);
        }
        
        return ResponseEntity.ok(response);
    }

    // Get current logged-in user info
    @GetMapping("/me")
    public Map<String, Object> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> userInfo = new HashMap<>();
        userService.populateRole(user);
        userInfo.put("id", user.getUserId());
        userInfo.put("email", user.getEmail());
        userInfo.put("firstName", user.getFirstName());
        userInfo.put("lastName", user.getLastName());
        userInfo.put("displayName", user.getDisplayName());
        userInfo.put("role", user.getRole());
        userInfo.put("contactNumber", user.getContactNumber());
        userInfo.put("status", user.getStatus());
        // Look up merchantId from merchants table (userId FK)
        Merchant merchant = merchantRepository.findByUserId(user.getUserId()).orElse(null);
        userInfo.put("merchantId", merchant != null ? merchant.getMerchantId() : null);
        userInfo.put("mfaEnabled", user.isMfaEnabled());
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
                    "message", "MFA is not configured for this account. Please set up MFA in your profile."
            ));
        }

        // Verify TOTP code using Google Authenticator compatible algorithm
        boolean isValid = totpService.verifyCode(user.getSecretKey(), otpCode);
        
        Map<String, Object> response = new HashMap<>();
        if (isValid) {
            response.put("success", true);
            response.put("message", "MFA verification successful");
        } else {
            response.put("success", false);
            response.put("message", "Invalid verification code. Please try again.");
        }
        return ResponseEntity.ok(response);
    }
}