package com.msp.backend.modules.merchant;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "merchants")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String businessName;

    @Column(nullable = false, unique = true)
    private String businessRegistrationNumber; // e.g., UEN or Tax ID

    @Column(nullable = false, unique = true)
    private String email;

    private String phoneNumber;

    private String address;

    private String status; // ACTIVE, SUSPENDED, PENDING

    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
    }
}