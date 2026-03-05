package com.msp.backend.modules.analytics;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "analytics")
public class Analytics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId; // FK to merchants

    @Column(name = "data_name", nullable = false)
    private String dataName; // e.g., TOTAL_SALES, DECLINE_RATE, AVG_TXN

    @Column(name = "data_value")
    private String dataValue;

    @Column(name = "generated_at")
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.generatedAt == null) this.generatedAt = LocalDateTime.now();
    }
}
