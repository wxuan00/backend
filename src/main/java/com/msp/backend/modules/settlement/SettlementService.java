package com.msp.backend.modules.settlement;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;

    public List<Settlement> getAllSettlements() {
        return settlementRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Settlement> getSettlementsByMerchantId(Long merchantId) {
        return settlementRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    public Settlement getSettlementById(Long id) {
        return settlementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Settlement not found"));
    }

    public List<Settlement> searchSettlements(String keyword) {
        return settlementRepository.findByMerchantNameContainingIgnoreCaseOrSettlementRefContainingIgnoreCase(
                keyword, keyword);
    }

    public List<Settlement> searchSettlementsByMerchant(Long merchantId, String keyword) {
        return settlementRepository.findByMerchantIdAndMerchantNameContainingIgnoreCase(merchantId, keyword);
    }
}
