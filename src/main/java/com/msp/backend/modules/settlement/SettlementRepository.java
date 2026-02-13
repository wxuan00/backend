package com.msp.backend.modules.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    List<Settlement> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    List<Settlement> findAllByOrderByCreatedAtDesc();

    List<Settlement> findByMerchantNameContainingIgnoreCaseOrSettlementRefContainingIgnoreCase(
            String merchantName, String settlementRef);

    List<Settlement> findByMerchantIdAndMerchantNameContainingIgnoreCase(Long merchantId, String merchantName);
}
