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
    @Column(name = "transaction_id")
    private Long transactionId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "settlement_id")
    private Long settlementId; // FK to settlements

    @Column(name = "payment_channel")
    private String paymentChannel; // e.g., POS, ONLINE, MOBILE

    @Column(nullable = false)
    private String status; // APPROVED, PENDING, DECLINED

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "txn_date")
    private LocalDateTime txnDate;

    @Column(name = "ref_no")
    private String refNo; // Reference number

    @Column(name = "card_no")
    private String cardNo; // Masked card number

    @Column(nullable = false)
    private String currency;

    @Column(name = "posted_date")
    private LocalDateTime postedDate;

    @Column(name = "txn_description")
    private String txnDescription;

    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "nett_amount", precision = 12, scale = 2)
    private BigDecimal nettAmount;

    // Transient field for display - populated from Merchant
    @Transient
    private String merchantName;

    @PrePersist
    protected void onCreate() {
        if (this.txnDate == null) this.txnDate = LocalDateTime.now();
        if (this.status == null) this.status = "PENDING";
        if (this.currency == null) this.currency = "MYR";
    }
}
