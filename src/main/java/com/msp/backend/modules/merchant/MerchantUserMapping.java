package com.msp.backend.modules.merchant;

import com.msp.backend.modules.user.User;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "merchant_users",
       uniqueConstraints = @UniqueConstraint(columnNames = {"merchant_id", "user_id"}))
public class MerchantUserMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    // JPA relationship for ERD FK: merchant_users.merchant_id → merchants
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_mu_merchant"))
    private Merchant merchant;

    // JPA relationship for ERD FK: merchant_users.user_id → users
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false,
                foreignKey = @ForeignKey(name = "fk_mu_user"))
    private User user;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_date")
    private LocalDateTime createdDate;

    @Column(name = "last_modified_by")
    private String lastModifiedBy;

    @Column(name = "last_modified_date")
    private LocalDateTime lastModifiedDate;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdDate == null) this.createdDate = now;
        if (this.lastModifiedDate == null) this.lastModifiedDate = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastModifiedDate = LocalDateTime.now();
    }
}
