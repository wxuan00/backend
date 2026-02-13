package com.msp.backend.modules.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditAdviceService {

    private final CreditAdviceRepository creditAdviceRepository;

    public List<CreditAdvice> getAllCreditAdvices() {
        return creditAdviceRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<CreditAdvice> getCreditAdvicesByMerchantId(Long merchantId) {
        return creditAdviceRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public CreditAdvice getCreditAdviceById(Long id) {
        return creditAdviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credit advice not found"));
    }

    public List<CreditAdvice> searchCreditAdvices(String keyword) {
        return creditAdviceRepository.findByMerchantNameContainingIgnoreCaseOrAdviceRefContainingIgnoreCase(
                keyword, keyword);
    }

    public List<CreditAdvice> searchCreditAdvicesByMerchant(Long merchantId, String keyword) {
        return creditAdviceRepository.findByMerchantIdAndMerchantNameContainingIgnoreCase(merchantId, keyword);
    }

    public List<CreditAdvice> getCreditAdvicesBySettlement(Long settlementId) {
        return creditAdviceRepository.findBySettlementId(settlementId);
    }
}
