package com.msp.backend.modules.analytics;

import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.settlement.SettlementRepository;
import com.msp.backend.modules.transaction.TransactionRepository;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {
        User currentUser = getCurrentUser();
        Map<String, Object> stats = new HashMap<>();

        if ("ADMIN".equals(currentUser.getRole())) {
            // Admin sees everything
            List<User> users = userRepository.findByDeletedAtIsNull();
            stats.put("totalUsers", users.size());

            // Recent users (last 5)
            List<Map<String, Object>> recentUsers = users.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .limit(5)
                    .map(u -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", u.getId());
                        m.put("firstName", u.getFirstName());
                        m.put("lastName", u.getLastName());
                        m.put("displayName", u.getDisplayName());
                        m.put("email", u.getEmail());
                        m.put("role", u.getRole());
                        m.put("status", u.getStatus());
                        return m;
                    })
                    .toList();
            stats.put("recentUsers", recentUsers);

            var allMerchants = merchantRepository.findAll();
            stats.put("totalMerchants", allMerchants.size());
            stats.put("activeMerchants", allMerchants.stream().filter(m -> "ACTIVE".equals(m.getStatus())).count());
            stats.put("pendingMerchants", allMerchants.stream().filter(m -> "PENDING".equals(m.getStatus())).count());

            stats.put("totalTransactions", transactionRepository.count());
            stats.put("totalSettlements", settlementRepository.count());
        } else {
            // Merchant user sees only their own data
            Long merchantId = currentUser.getMerchantId();
            if (merchantId != null) {
                var myMerchant = merchantRepository.findById(merchantId);
                stats.put("totalMerchants", myMerchant.isPresent() ? 1 : 0);
                stats.put("activeMerchants", myMerchant.filter(m -> "ACTIVE".equals(m.getStatus())).isPresent() ? 1 : 0);
                stats.put("pendingMerchants", myMerchant.filter(m -> "PENDING".equals(m.getStatus())).isPresent() ? 1 : 0);

                var myTransactions = transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
                stats.put("totalTransactions", myTransactions.size());

                var mySettlements = settlementRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
                stats.put("totalSettlements", mySettlements.size());
            } else {
                stats.put("totalMerchants", 0);
                stats.put("activeMerchants", 0);
                stats.put("pendingMerchants", 0);
                stats.put("totalTransactions", 0);
                stats.put("totalSettlements", 0);
            }
        }

        return stats;
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
