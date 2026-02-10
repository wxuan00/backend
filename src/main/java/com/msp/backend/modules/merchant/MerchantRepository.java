package com.msp.backend.modules.merchant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {
    // For the Search feature (UC09)
    List<Merchant> findByBusinessNameContainingIgnoreCase(String name);
}