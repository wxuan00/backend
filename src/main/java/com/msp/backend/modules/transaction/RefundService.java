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
public class RefundService {

    private final RefundRepository refundRepository;

    public List<Refund> getAllRefunds() {
        return refundRepository.findAllByOrderBySubmissionDateDesc();
    }

    public List<Refund> getRefundsByMerchantId(Long merchantId) {
        return refundRepository.findByMerchantIdOrderBySubmissionDateDesc(merchantId);
    }

    public Refund getRefundById(Long id) {
        return refundRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Refund not found"));
    }

    public Refund requestRefund(Refund refund) {
        refund.setStatus("PENDING");
        return refundRepository.save(refund);
    }

    public Refund cancelRefund(Long id) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Refund not found"));
        if (!"PENDING".equals(refund.getStatus())) {
            throw new RuntimeException("Only PENDING refunds can be cancelled");
        }
        refund.setStatus("CANCELLED");
        return refundRepository.save(refund);
    }

    public Page<Refund> getRefundsPage(
            Long restrictToMerchantId,
            String merchantName,
            String refundRefNo,
            String cardNo,
            String status,
            String refundType,
            String dateFrom,
            String dateTo,
            int page,
            int size,
            String sortBy,
            String sortDir
    ) {
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

        Specification<Refund> spec = (root, query, cb) -> {
            var merchantJoin = root.join("merchant", jakarta.persistence.criteria.JoinType.LEFT);
            if (query != null) query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (restrictToMerchantId != null) {
                predicates.add(cb.equal(root.get("merchantId"), restrictToMerchantId));
            }
            if (merchantName != null && !merchantName.isBlank()) {
                predicates.add(cb.like(cb.lower(merchantJoin.get("merchantName")), "%" + merchantName.toLowerCase().trim() + "%"));
            }
            if (refundRefNo != null && !refundRefNo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("refundRefNo")), "%" + refundRefNo.toLowerCase().trim() + "%"));
            }
            if (cardNo != null && !cardNo.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("cardNo")), "%" + cardNo.toLowerCase().trim() + "%"));
            }
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (refundType != null && !refundType.isBlank()) {
                predicates.add(cb.equal(root.get("refundType"), refundType));
            }
            if (dateFrom != null && !dateFrom.isBlank()) {
                LocalDateTime from = LocalDate.parse(dateFrom).atStartOfDay();
                predicates.add(cb.greaterThanOrEqualTo(root.get("submissionDate"), from));
            }
            if (dateTo != null && !dateTo.isBlank()) {
                LocalDateTime to = LocalDate.parse(dateTo).atTime(LocalTime.MAX);
                predicates.add(cb.lessThanOrEqualTo(root.get("submissionDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return refundRepository.findAll(spec, pageable);
    }
}
