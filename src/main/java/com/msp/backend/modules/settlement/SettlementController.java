package com.msp.backend.modules.settlement;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
public class SettlementController {

    private final SettlementService settlementService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantRepository merchantRepository;

    @GetMapping
    public Page<Settlement> getAllSettlements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "settlementDate") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) String merchantName,
            @RequestParam(required = false) String settlementNo,
            @RequestParam(required = false) String settlementType,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo
    ) {
        User currentUser = getCurrentUser();
        Long restrictToMerchantId = null;
        if (!"ADMIN".equals(currentUser.getRole())) {
            restrictToMerchantId = getMyMerchantId(currentUser);
            if (restrictToMerchantId == null) return Page.empty();
        }
        return settlementService.getSettlementsPage(
                restrictToMerchantId, merchantName, settlementNo, settlementType, dateFrom, dateTo,
                page, size, sortBy, sortDir);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Settlement> getSettlementById(@PathVariable Long id) {
        Settlement settlement = settlementService.getSettlementById(id);
        User currentUser = getCurrentUser();

        if (!"ADMIN".equals(currentUser.getRole())) {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null || !merchantId.equals(settlement.getMerchantId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(settlement);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        userService.populateRole(user);
        return user;
    }

    private Long getMyMerchantId(User user) {
        return merchantRepository.findByUserId(user.getUserId())
                .map(Merchant::getMerchantId)
                .orElse(null);
    }
}
