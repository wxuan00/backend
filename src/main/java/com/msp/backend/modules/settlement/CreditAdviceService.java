package com.msp.backend.modules.settlement;

import com.msp.backend.modules.merchant.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CreditAdviceService {

    private final CreditAdviceRepository creditAdviceRepository;
    private final MerchantRepository merchantRepository;

    public List<CreditAdvice> getAllCreditAdvices() {
        List<CreditAdvice> advices = creditAdviceRepository.findAllByOrderByPaymentDateDesc();
        advices.forEach(this::populateMerchantName);
        return advices;
    }

    public List<CreditAdvice> getCreditAdvicesByMerchantId(Long merchantId) {
        List<CreditAdvice> advices = creditAdviceRepository.findByMerchantIdOrderByPaymentDateDesc(merchantId);
        advices.forEach(this::populateMerchantName);
        return advices;
    }

    public CreditAdvice getCreditAdviceById(Long id) {
        CreditAdvice advice = creditAdviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Credit advice not found"));
        populateMerchantName(advice);
        return advice;
    }

    private void populateMerchantName(CreditAdvice advice) {
        if (advice.getMerchantId() != null) {
            merchantRepository.findById(advice.getMerchantId()).ifPresent(
                m -> advice.setMerchantName(m.getMerchantName())
            );
        }
    }
}
