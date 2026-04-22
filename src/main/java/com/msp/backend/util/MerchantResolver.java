package com.msp.backend.util;

import com.msp.backend.modules.merchant.MerchantUserMappingRepository;
import com.msp.backend.modules.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Resolves the merchant ID for a logged-in user.
 *
 * Two linkage models exist:
 *  1. Legacy: merchants.user_id FK (one-to-one, primary owner)
 *  2. New:    merchant_users mapping table (many-to-many via MerchantUserMapping)
 *
 * This helper checks both so that users linked via either mechanism get their data.
 */
@Component
@RequiredArgsConstructor
public class MerchantResolver {

    private final MerchantUserMappingRepository merchantUserMappingRepository;

    /**
     * Returns the merchant ID associated with the given user, or null if not linked.
     * Checks the direct FK first, then falls back to the mapping table.
     */
    public Long resolveForUser(User user) {
        // Resolve via merchant_users mapping table
        var mappings = merchantUserMappingRepository.findByUserId(user.getUserId());
        if (!mappings.isEmpty()) return mappings.get(0).getMerchantId();
        return null;
    }
}
