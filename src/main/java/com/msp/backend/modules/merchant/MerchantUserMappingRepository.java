package com.msp.backend.modules.merchant;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface MerchantUserMappingRepository extends JpaRepository<MerchantUserMapping, Long> {
    List<MerchantUserMapping> findByMerchantId(Long merchantId);
    Optional<MerchantUserMapping> findByMerchantIdAndUserId(Long merchantId, Long userId);
    boolean existsByMerchantIdAndUserId(Long merchantId, Long userId);
    void deleteByMerchantIdAndUserId(Long merchantId, Long userId);
    List<MerchantUserMapping> findByUserId(Long userId);
}
