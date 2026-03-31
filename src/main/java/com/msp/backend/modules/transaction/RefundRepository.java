package com.msp.backend.modules.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long>,
        JpaSpecificationExecutor<Refund> {

    List<Refund> findByMerchantIdOrderBySubmissionDateDesc(Long merchantId);

    List<Refund> findAllByOrderBySubmissionDateDesc();

    List<Refund> findByTransactionId(Long transactionId);

    Page<Refund> findByMerchantId(Long merchantId, Pageable pageable);
}
