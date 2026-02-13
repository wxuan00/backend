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
@RequestMapping("/api/credit-advices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CreditAdviceController {

    private final CreditAdviceService creditAdviceService;
    private final UserRepository userRepository;

    @GetMapping
    public List<CreditAdvice> getAllCreditAdvices() {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return creditAdviceService.getAllCreditAdvices();
        } else {
            if (currentUser.getMerchantId() == null) return List.of();
            return creditAdviceService.getCreditAdvicesByMerchantId(currentUser.getMerchantId());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreditAdvice> getCreditAdviceById(@PathVariable Long id) {
        CreditAdvice advice = creditAdviceService.getCreditAdviceById(id);
        User currentUser = getCurrentUser();

        if (!"ADMIN".equals(currentUser.getRole())) {
            if (currentUser.getMerchantId() == null || !currentUser.getMerchantId().equals(advice.getMerchantId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(advice);
    }

    @GetMapping("/search")
    public List<CreditAdvice> searchCreditAdvices(@RequestParam String keyword) {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return creditAdviceService.searchCreditAdvices(keyword);
        } else {
            if (currentUser.getMerchantId() == null) return List.of();
            return creditAdviceService.searchCreditAdvicesByMerchant(currentUser.getMerchantId(), keyword);
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
