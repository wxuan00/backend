package com.msp.backend.modules.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CreditAdviceRepository extends JpaRepository<CreditAdvice, Long> {

    List<CreditAdvice> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    List<CreditAdvice> findAllByOrderByCreatedAtDesc();

    List<CreditAdvice> findByMerchantNameContainingIgnoreCaseOrAdviceRefContainingIgnoreCase(
            String merchantName, String adviceRef);

    List<CreditAdvice> findByMerchantIdAndMerchantNameContainingIgnoreCase(Long merchantId, String merchantName);

    List<CreditAdvice> findBySettlementId(Long settlementId);
}
