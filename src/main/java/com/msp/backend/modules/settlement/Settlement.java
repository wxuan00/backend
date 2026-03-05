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
    @Column(name = "settlement_id")
    private Long settlementId;

    @Column(name = "credit_advice_id")
    private Long creditAdviceId; // FK to credit_advices

    @Column(name = "settlement_date")
    private LocalDateTime settlementDate;

    @Column(name = "settlement_no", unique = true)
    private String settlementNo;

    @Column(name = "settlement_type")
    private String settlementType; // e.g., DAILY, WEEKLY

    @Column(nullable = false)
    private String currency;

    @Column(name = "settlement_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal settlementAmount;

    @Column(name = "payment_amount", precision = 14, scale = 2)
    private BigDecimal paymentAmount;

    // Transient fields for display
    @Transient
    private String merchantName;

    @Transient
    private Long merchantId;

    @PrePersist
    protected void onCreate() {
        if (this.settlementDate == null) this.settlementDate = LocalDateTime.now();
        if (this.currency == null) this.currency = "MYR";
    }
}
