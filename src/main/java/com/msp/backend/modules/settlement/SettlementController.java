package com.msp.backend.modules.settlement;

import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/settlements")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SettlementController {

    private final SettlementService settlementService;
    private final UserRepository userRepository;

    @GetMapping
    public List<Settlement> getAllSettlements() {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return settlementService.getAllSettlements();
        } else {
            if (currentUser.getMerchantId() == null) return List.of();
            return settlementService.getSettlementsByMerchantId(currentUser.getMerchantId());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Settlement> getSettlementById(@PathVariable Long id) {
        Settlement settlement = settlementService.getSettlementById(id);
        User currentUser = getCurrentUser();

        if (!"ADMIN".equals(currentUser.getRole())) {
            if (currentUser.getMerchantId() == null || !currentUser.getMerchantId().equals(settlement.getMerchantId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(settlement);
    }

    @GetMapping("/search")
    public List<Settlement> searchSettlements(@RequestParam String keyword) {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return settlementService.searchSettlements(keyword);
        } else {
            if (currentUser.getMerchantId() == null) return List.of();
            return settlementService.searchSettlementsByMerchant(currentUser.getMerchantId(), keyword);
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
