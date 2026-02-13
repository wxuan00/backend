package com.msp.backend.modules.settlement;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "credit_advices")
public class CreditAdvice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "advice_ref", unique = true)
    private String adviceRef; // Unique credit advice reference

    @Column(name = "settlement_id")
    private Long settlementId; // Links to a settlement

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String currency; // MYR, SGD, USD

    @Column(nullable = false)
    private String status; // PENDING, ISSUED, ACKNOWLEDGED

    @Column(name = "bank_account")
    private String bankAccount; // Masked bank account for payout

    @Column(name = "bank_name")
    private String bankName;

    private String remarks;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
        if (this.currency == null) this.currency = "MYR";
    }
}
