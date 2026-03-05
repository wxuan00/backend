package com.msp.backend.modules.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByMerchantIdOrderBySubmissionDateDesc(Long merchantId);

    List<Refund> findAllByOrderBySubmissionDateDesc();

    List<Refund> findByTransactionId(Long transactionId);
}
