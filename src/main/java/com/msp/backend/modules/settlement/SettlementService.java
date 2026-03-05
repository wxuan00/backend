package com.msp.backend.modules.settlement;

import com.msp.backend.modules.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SettlementService {

    private final SettlementRepository settlementRepository;
    private final CreditAdviceRepository creditAdviceRepository;
    private final MerchantRepository merchantRepository;

    public List<Settlement> getAllSettlements() {
        List<Settlement> settlements = settlementRepository.findAllByOrderBySettlementDateDesc();
        settlements.forEach(this::populateMerchantInfo);
        return settlements;
    }

    public List<Settlement> getSettlementsByMerchantId(Long merchantId) {
        // Find credit advices for this merchant, then find settlements linked to those
        List<CreditAdvice> advices = creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(merchantId);
        List<Long> adviceIds = advices.stream().map(CreditAdvice::getCreditAdviceId).toList();
        if (adviceIds.isEmpty()) return List.of();
        List<Settlement> settlements = settlementRepository.findByCreditAdviceIdInOrderBySettlementDateDesc(adviceIds);
        settlements.forEach(this::populateMerchantInfo);
        return settlements;
    }

    public Settlement getSettlementById(Long id) {
        Settlement settlement = settlementRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Settlement not found"));
        populateMerchantInfo(settlement);
        return settlement;
    }

    private void populateMerchantInfo(Settlement settlement) {
        if (settlement.getCreditAdviceId() != null) {
            creditAdviceRepository.findById(settlement.getCreditAdviceId()).ifPresent(ca -> {
                settlement.setMerchantId(ca.getMerchantId());
                merchantRepository.findById(ca.getMerchantId()).ifPresent(
                    m -> settlement.setMerchantName(m.getMerchantName())
                );
            });
        }
    }
}
