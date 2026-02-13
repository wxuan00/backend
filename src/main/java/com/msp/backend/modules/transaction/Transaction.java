package com.msp.backend.modules.transaction;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(nullable = false)
    private String type; // SALE, REFUND, VOID

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency; // MYR, SGD, USD

    @Column(nullable = false)
    private String status; // APPROVED, PENDING, DECLINED

    @Column(name = "card_last4")
    private String cardLast4;

    @Column(name = "card_brand")
    private String cardBrand; // Visa, Mastercard

    @Column(name = "auth_code")
    private String authCode;

    private String rrn; // Retrieval Reference Number

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
        if (this.currency == null) this.currency = "MYR";
    }
}
