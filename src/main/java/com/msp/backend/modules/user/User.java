package com.msp.backend.modules.user;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "users") // Maps to SQL table 'users'
@Data // Lombok: Auto-generates Getters, Setters, ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // Will be hashed (e.g., BCrypt)

    @Column(name = "display_name")
    private String displayName;

    @Column(nullable = false)
    private String role; // 'ADMIN' or 'MERCHANT'

    @Column(name = "is_mfa_enabled")
    private boolean isMfaEnabled = false;

    @Column(name = "secret_key")
    private String secretKey; // For Google Authenticator 2FA

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
