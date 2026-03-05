package com.msp.backend.modules.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final MerchantNameResolver merchantNameResolver;

    public List<Transaction> getAllTransactions() {
        List<Transaction> txns = transactionRepository.findAllByOrderByTxnDateDesc();
        txns.forEach(t -> t.setMerchantName(merchantNameResolver.resolve(t.getMerchantId())));
        return txns;
    }

    public List<Transaction> getTransactionsByMerchantId(Long merchantId) {
        List<Transaction> txns = transactionRepository.findByMerchantIdOrderByTxnDateDesc(merchantId);
        txns.forEach(t -> t.setMerchantName(merchantNameResolver.resolve(t.getMerchantId())));
        return txns;
    }

    public Transaction getTransactionById(Long id) {
        Transaction txn = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        txn.setMerchantName(merchantNameResolver.resolve(txn.getMerchantId()));
        return txn;
    }
}
