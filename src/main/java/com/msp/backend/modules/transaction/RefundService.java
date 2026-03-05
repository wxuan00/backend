package com.msp.backend.modules.transaction;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRepository refundRepository;
    private final MerchantNameResolver merchantNameResolver;

    public List<Refund> getAllRefunds() {
        List<Refund> refunds = refundRepository.findAllByOrderBySubmissionDateDesc();
        refunds.forEach(r -> r.setMerchantName(merchantNameResolver.resolve(r.getMerchantId())));
        return refunds;
    }

    public List<Refund> getRefundsByMerchantId(Long merchantId) {
        List<Refund> refunds = refundRepository.findByMerchantIdOrderBySubmissionDateDesc(merchantId);
        refunds.forEach(r -> r.setMerchantName(merchantNameResolver.resolve(r.getMerchantId())));
        return refunds;
    }

    public Refund getRefundById(Long id) {
        Refund refund = refundRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Refund not found"));
        refund.setMerchantName(merchantNameResolver.resolve(refund.getMerchantId()));
        return refund;
    }
}
