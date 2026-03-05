package com.msp.backend.modules.transaction;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {

    private final RefundService refundService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantRepository merchantRepository;

    @GetMapping
    public List<Refund> getAllRefunds() {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return refundService.getAllRefunds();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null) return List.of();
            return refundService.getRefundsByMerchantId(merchantId);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Refund> getRefundById(@PathVariable Long id) {
        Refund refund = refundService.getRefundById(id);
        User currentUser = getCurrentUser();

        if (!"ADMIN".equals(currentUser.getRole())) {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null || !merchantId.equals(refund.getMerchantId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(refund);
    }

    @GetMapping("/search")
    public List<Refund> searchRefunds(@RequestParam String keyword) {
        User currentUser = getCurrentUser();
        List<Refund> all;
        if ("ADMIN".equals(currentUser.getRole())) {
            all = refundService.getAllRefunds();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null) return List.of();
            all = refundService.getRefundsByMerchantId(merchantId);
        }
        String kw = keyword.toLowerCase();
        return all.stream().filter(r ->
            (r.getMerchantName() != null && r.getMerchantName().toLowerCase().contains(kw)) ||
            (r.getRefundRefNo() != null && r.getRefundRefNo().toLowerCase().contains(kw)) ||
            (r.getCardNo() != null && r.getCardNo().toLowerCase().contains(kw))
        ).toList();
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
