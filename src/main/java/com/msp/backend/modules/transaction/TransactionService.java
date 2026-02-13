package com.msp.backend.modules.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> getAllTransactions() {
        return transactionRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Transaction> getTransactionsByMerchantId(Long merchantId) {
        return transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public List<Transaction> searchTransactions(String keyword) {
        return transactionRepository.findByMerchantNameContainingIgnoreCaseOrRrnContaining(keyword, keyword);
    }

    public List<Transaction> searchTransactionsByMerchant(Long merchantId, String keyword) {
        return transactionRepository.findByMerchantIdAndMerchantNameContainingIgnoreCase(merchantId, keyword);
    }
}
