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

    public Merchant getMerchantById(Long id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));
    }

    public Merchant createMerchant(Merchant merchant) {
        return merchantRepository.save(merchant);
    }

    public Merchant updateMerchant(Long id, Merchant updated) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        if (updated.getBusinessName() != null) merchant.setBusinessName(updated.getBusinessName());
        if (updated.getBusinessRegistrationNumber() != null) merchant.setBusinessRegistrationNumber(updated.getBusinessRegistrationNumber());
        if (updated.getEmail() != null) merchant.setEmail(updated.getEmail());
        if (updated.getPhoneNumber() != null) merchant.setPhoneNumber(updated.getPhoneNumber());
        if (updated.getAddress() != null) merchant.setAddress(updated.getAddress());
        if (updated.getStatus() != null) merchant.setStatus(updated.getStatus());

        return merchantRepository.save(merchant);
    }

    public void deleteMerchant(Long id) {
        Merchant merchant = merchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));
        merchantRepository.delete(merchant);
    }

    public List<Merchant> searchMerchants(String keyword) {
        return merchantRepository.findByBusinessNameContainingIgnoreCase(keyword);
    }
}