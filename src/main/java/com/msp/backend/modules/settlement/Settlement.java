package com.msp.backend.modules.settlement;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "settlements")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "settlement_ref", unique = true)
    private String settlementRef; // Unique settlement reference number

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 14, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "net_amount", precision = 14, scale = 2)
    private BigDecimal netAmount;

    @Column(nullable = false)
    private String currency; // MYR, SGD, USD

    @Column(nullable = false)
    private String status; // PENDING, PROCESSED, PAID, FAILED

    @Column(name = "transaction_count")
    private Integer transactionCount;

    @Column(name = "period_start")
    private LocalDateTime periodStart;

    @Column(name = "period_end")
    private LocalDateTime periodEnd;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
        if (this.currency == null) this.currency = "MYR";
        if (this.netAmount == null && this.totalAmount != null && this.feeAmount != null) {
            this.netAmount = this.totalAmount.subtract(this.feeAmount);
        }
    }
}
