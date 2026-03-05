package com.msp.backend.modules.merchant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    List<Merchant> findByMerchantNameContainingIgnoreCase(String name);

    Optional<Merchant> findByUserId(Long userId);
}