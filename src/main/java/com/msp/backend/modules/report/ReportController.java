package com.msp.backend.modules.report;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.settlement.Settlement;
import com.msp.backend.modules.settlement.SettlementService;
import com.msp.backend.modules.transaction.Transaction;
import com.msp.backend.modules.transaction.TransactionService;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantRepository merchantRepository;
    private final TransactionService transactionService;
    private final SettlementService settlementService;
    private final FileGeneratorService fileGeneratorService;

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummaryReport() {
        User currentUser = getCurrentUser();
        Map<String, Object> report = buildSummaryReport(currentUser);
        return ResponseEntity.ok(report);
    }

    private Map<String, Object> buildSummaryReport(User currentUser) {
        Map<String, Object> report = new HashMap<>();
        report.put("generatedBy", currentUser.getEmail());
        report.put("generatedAt", java.time.LocalDateTime.now().toString());

        if ("ADMIN".equals(currentUser.getRole())) {
            report.put("type", "ADMIN_SUMMARY");
            report.put("totalUsers", userRepository.findByDeletedAtIsNull().size());
            report.put("totalMerchants", merchantRepository.count());

            var merchants = merchantRepository.findAll();
            report.put("activeMerchants", merchants.stream().filter(m -> "ACTIVE".equals(m.getStatus())).count());
            report.put("pendingMerchants", merchants.stream().filter(m -> "PENDING".equals(m.getStatus())).count());
            report.put("suspendedMerchants", merchants.stream().filter(m -> "SUSPENDED".equals(m.getStatus())).count());

            var transactions = transactionService.getAllTransactions();
            report.put("totalTransactions", transactions.size());
            report.put("approvedTransactions", transactions.stream().filter(t -> "APPROVED".equals(t.getStatus())).count());
            report.put("pendingTransactions", transactions.stream().filter(t -> "PENDING".equals(t.getStatus())).count());
            report.put("declinedTransactions", transactions.stream().filter(t -> "DECLINED".equals(t.getStatus())).count());

            report.put("totalSettlements", settlementService.getAllSettlements().size());
        } else {
            report.put("type", "MERCHANT_SUMMARY");
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId != null) {
                var merchant = merchantRepository.findById(merchantId);
                report.put("merchantName", merchant.map(Merchant::getMerchantName).orElse("Unknown"));
                report.put("merchantStatus", merchant.map(Merchant::getStatus).orElse("Unknown"));

                var transactions = transactionService.getTransactionsByMerchantId(merchantId);
                report.put("totalTransactions", transactions.size());
                report.put("approvedTransactions", transactions.stream().filter(t -> "APPROVED".equals(t.getStatus())).count());
                report.put("pendingTransactions", transactions.stream().filter(t -> "PENDING".equals(t.getStatus())).count());
                report.put("declinedTransactions", transactions.stream().filter(t -> "DECLINED".equals(t.getStatus())).count());

                var settlements = settlementService.getSettlementsByMerchantId(merchantId);
                report.put("totalSettlements", settlements.size());
            }
        }

        return report;
    }

    @GetMapping("/summary/export")
    public ResponseEntity<byte[]> exportSummaryReport() {
        User currentUser = getCurrentUser();
        Map<String, Object> report = buildSummaryReport(currentUser);
        byte[] csv = fileGeneratorService.generateSummaryReportCsv(report);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=summary-report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/transactions/export")
    public ResponseEntity<byte[]> exportTransactions() {
        User currentUser = getCurrentUser();
        List<Transaction> transactions;
        if ("ADMIN".equals(currentUser.getRole())) {
            transactions = transactionService.getAllTransactions();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            transactions = merchantId != null
                    ? transactionService.getTransactionsByMerchantId(merchantId)
                    : List.of();
        }
        byte[] csv = fileGeneratorService.generateTransactionsCsv(transactions);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=transactions-export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @GetMapping("/settlements/export")
    public ResponseEntity<byte[]> exportSettlements() {
        User currentUser = getCurrentUser();
        List<Settlement> settlements;
        if ("ADMIN".equals(currentUser.getRole())) {
            settlements = settlementService.getAllSettlements();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            settlements = merchantId != null
                    ? settlementService.getSettlementsByMerchantId(merchantId)
                    : List.of();
        }
        byte[] csv = fileGeneratorService.generateSettlementsCsv(settlements);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=settlements-export.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
        userService.populateRole(user);
        return user;
    }

    private Long getMyMerchantId(User user) {
        return merchantRepository.findByUserId(user.getUserId())
                .map(Merchant::getMerchantId)
                .orElse(null);
    }
}
