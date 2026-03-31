package com.msp.backend.modules.transaction;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;

    public List<Transaction> getAllTransactions() {
        List<Transaction> txns = transactionRepository.findAllByOrderByTxnDateDesc();
        return txns;
    }

    public List<Transaction> getTransactionsByMerchantId(Long merchantId) {
        List<Transaction> txns = transactionRepository.findByMerchantIdOrderByTxnDateDesc(merchantId);
        return txns;
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
    }

    public Page<Transaction> getTransactionsPage(
            Long restrictToMerchantId,
            String merchantName,
            String txnId,
            String cardNo,
            String status,
            String channel,
            String dateFrom,
            String dateTo,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
        // Merchant name sort goes through the join path
        Sort sort;
        if ("merchantName".equals(sortBy)) {
            sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by("merchant.merchantName").ascending()
                    : Sort.by("merchant.merchantName").descending();
        } else {
            sort = sortDir.equalsIgnoreCase("asc")
                    ? Sort.by(sortBy).ascending()
                    : Sort.by(sortBy).descending();
        }
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Transaction> spec = (root, query, cb) -> {
            // Always join merchant so sorting by name works
            var merchantJoin = root.join("merchant", jakarta.persistence.criteria.JoinType.LEFT);
            if (query != null) query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (restrictToMerchantId != null) {
                predicates.add(cb.equal(root.get("merchantId"), restrictToMerchantId));
            }
            if (merchantName != null && !merchantName.isBlank()) {
                predicates.add(cb.like(cb.lower(merchantJoin.get("merchantName")), "%" + merchantName.toLowerCase().trim() + "%"));
            }
            if (txnId != null && !txnId.isBlank()) {
                predicates.add(cb.like(cb.toString(root.get("transactionId")), "%" + txnId.trim() + "%"));
            }
            if (cardNo != null && !cardNo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("cardNo")), "%" + cardNo.toLowerCase().trim() + "%"));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (channel != null && !channel.isBlank()) {
                predicates.add(cb.equal(root.get("paymentChannel"), channel));
            }
            if (dateFrom != null && !dateFrom.isBlank()) {
                LocalDateTime from = LocalDate.parse(dateFrom).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("txnDate"), from));
            }
            if (dateTo != null && !dateTo.isBlank()) {
                LocalDateTime to = LocalDate.parse(dateTo).atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("txnDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return transactionRepository.findAll(spec, pageable);
    }
}
