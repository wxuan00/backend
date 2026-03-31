package com.msp.backend.modules.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>,
        JpaSpecificationExecutor<Transaction> {

    List<Transaction> findByMerchantIdOrderByTxnDateDesc(Long merchantId);

    List<Transaction> findAllByOrderByTxnDateDesc();

    List<Transaction> findBySettlementId(Long settlementId);

    Page<Transaction> findByMerchantId(Long merchantId, Pageable pageable);
}
