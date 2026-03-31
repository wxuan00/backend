package com.msp.backend.modules.settlement;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface CreditAdviceRepository extends JpaRepository<CreditAdvice, Long>,
        JpaSpecificationExecutor<CreditAdvice> {

    List<CreditAdvice> findByMerchantIdOrderByPaymentDateDesc(Long merchantId);

    List<CreditAdvice> findAllByOrderByPaymentDateDesc();

    Page<CreditAdvice> findByMerchantId(Long merchantId, Pageable pageable);
}
