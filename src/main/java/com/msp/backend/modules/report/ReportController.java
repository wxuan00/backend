package com.msp.backend.modules.report;

import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.settlement.SettlementRepository;
import com.msp.backend.modules.transaction.TransactionRepository;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ReportController {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final SettlementRepository settlementRepository;

    /**
     * Generate a summary report.
     * Admin: full portal summary
     * Merchant: their own merchant summary
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummaryReport() {
        User currentUser = getCurrentUser();
        Map<String, Object> report = new HashMap<>();
        report.put("generatedBy", currentUser.getEmail());
        report.put("generatedAt", java.time.LocalDateTime.now().toString());

        if ("ADMIN".equals(currentUser.getRole())) {
            report.put("type", "ADMIN_SUMMARY");
            report.put("totalUsers", userRepository.findByDeletedAtIsNull().size());
            report.put("totalMerchants", merchantRepository.count());
            report.put("totalTransactions", transactionRepository.count());
            report.put("totalSettlements", settlementRepository.count());

            var merchants = merchantRepository.findAll();
            report.put("activeMerchants", merchants.stream().filter(m -> "ACTIVE".equals(m.getStatus())).count());
            report.put("pendingMerchants", merchants.stream().filter(m -> "PENDING".equals(m.getStatus())).count());
            report.put("suspendedMerchants", merchants.stream().filter(m -> "SUSPENDED".equals(m.getStatus())).count());

            var transactions = transactionRepository.findAllByOrderByCreatedAtDesc();
            report.put("approvedTransactions", transactions.stream().filter(t -> "APPROVED".equals(t.getStatus())).count());
            report.put("pendingTransactions", transactions.stream().filter(t -> "PENDING".equals(t.getStatus())).count());
            report.put("declinedTransactions", transactions.stream().filter(t -> "DECLINED".equals(t.getStatus())).count());
        } else {
            report.put("type", "MERCHANT_SUMMARY");
            Long merchantId = currentUser.getMerchantId();
            if (merchantId != null) {
                var merchant = merchantRepository.findById(merchantId);
                report.put("merchantName", merchant.map(m -> m.getBusinessName()).orElse("Unknown"));
                report.put("merchantStatus", merchant.map(m -> m.getStatus()).orElse("Unknown"));

                var transactions = transactionRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
                report.put("totalTransactions", transactions.size());
                report.put("approvedTransactions", transactions.stream().filter(t -> "APPROVED".equals(t.getStatus())).count());
                report.put("pendingTransactions", transactions.stream().filter(t -> "PENDING".equals(t.getStatus())).count());
                report.put("declinedTransactions", transactions.stream().filter(t -> "DECLINED".equals(t.getStatus())).count());

                var settlements = settlementRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
                report.put("totalSettlements", settlements.size());
            }
        }

        return ResponseEntity.ok(report);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}
