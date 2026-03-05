package com.msp.backend.modules.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByMerchantIdOrderByTxnDateDesc(Long merchantId);

    List<Transaction> findAllByOrderByTxnDateDesc();

    List<Transaction> findBySettlementId(Long settlementId);
}
