package com.msp.backend.modules.transaction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    List<Transaction> findAllByOrderByCreatedAtDesc();

    List<Transaction> findByMerchantNameContainingIgnoreCaseOrRrnContaining(String merchantName, String rrn);

    List<Transaction> findByMerchantIdAndMerchantNameContainingIgnoreCase(Long merchantId, String merchantName);
}
