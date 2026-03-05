package com.msp.backend.modules.settlement;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CreditAdviceRepository extends JpaRepository<CreditAdvice, Long> {

    List<CreditAdvice> findByMerchantIdOrderByPaymentDateDesc(Long merchantId);

    List<CreditAdvice> findAllByOrderByPaymentDateDesc();
}
