package com.msp.backend.modules.merchant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MerchantService {

    private final MerchantRepository merchantRepository;

    public List<Merchant> getAllMerchants() {
        return merchantRepository.findAll();
    }

    public Merchant createMerchant(Merchant merchant) {
        return merchantRepository.save(merchant);
    }

    public List<Merchant> searchMerchants(String keyword) {
        return merchantRepository.findByBusinessNameContainingIgnoreCase(keyword);
    }
}