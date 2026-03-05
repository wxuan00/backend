package com.msp.backend.modules.analytics;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.settlement.Settlement;
import com.msp.backend.modules.settlement.SettlementService;
import com.msp.backend.modules.transaction.Transaction;
import com.msp.backend.modules.transaction.TransactionRepository;
import com.msp.backend.modules.transaction.TransactionService;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final SettlementService settlementService;
    private final AIEngine aiEngine;

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {
        User currentUser = getCurrentUser();
        Map<String, Object> stats = new HashMap<>();

        if ("ADMIN".equals(currentUser.getRole())) {
            List<User> users = userRepository.findByDeletedAtIsNull();
            users.forEach(userService::populateRole);
            stats.put("totalUsers", users.size());

            List<Map<String, Object>> recentUsers = users.stream()
                    .sorted((a, b) -> {
                        if (a.getCreatedAt() == null) return 1;
                        if (b.getCreatedAt() == null) return -1;
                        return b.getCreatedAt().compareTo(a.getCreatedAt());
                    })
                    .limit(5)
                    .map(u -> {
                        Map<String, Object> m = new HashMap<>();
                        m.put("id", u.getUserId());
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
            stats.put("totalSettlements", settlementService.getAllSettlements().size());
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId != null) {
                var myMerchant = merchantRepository.findById(merchantId);
                stats.put("totalMerchants", myMerchant.isPresent() ? 1 : 0);
                stats.put("activeMerchants", myMerchant.filter(m -> "ACTIVE".equals(m.getStatus())).isPresent() ? 1 : 0);
                stats.put("pendingMerchants", myMerchant.filter(m -> "PENDING".equals(m.getStatus())).isPresent() ? 1 : 0);

                var myTransactions = transactionRepository.findByMerchantIdOrderByTxnDateDesc(merchantId);
                stats.put("totalTransactions", myTransactions.size());

                var mySettlements = settlementService.getSettlementsByMerchantId(merchantId);
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

    @GetMapping("/insights")
    public List<Map<String, Object>> getInsights() {
        User currentUser = getCurrentUser();

        if ("ADMIN".equals(currentUser.getRole())) {
            List<Transaction> transactions = transactionService.getAllTransactions();
            List<Merchant> merchants = merchantRepository.findAll();
            List<Settlement> settlements = settlementService.getAllSettlements();
            return aiEngine.generateInsights(transactions, merchants, settlements);
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null) return List.of();

            List<Transaction> transactions = transactionService.getTransactionsByMerchantId(merchantId);
            List<Settlement> settlements = settlementService.getSettlementsByMerchantId(merchantId);
            return aiEngine.generateMerchantInsights(transactions, settlements);
        }
    }

    @GetMapping("/chart-data")
    public Map<String, Object> getChartData() {
        User currentUser = getCurrentUser();
        Map<String, Object> charts = new HashMap<>();

        List<Transaction> transactions;
        List<Settlement> settlements;
        List<Merchant> merchants;

        if ("ADMIN".equals(currentUser.getRole())) {
            transactions = transactionService.getAllTransactions();
            settlements = settlementService.getAllSettlements();
            merchants = merchantRepository.findAll();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            transactions = merchantId != null
                    ? transactionService.getTransactionsByMerchantId(merchantId)
                    : List.of();
            settlements = merchantId != null
                    ? settlementService.getSettlementsByMerchantId(merchantId)
                    : List.of();
            merchants = List.of();
        }

        // 1. Transaction Status Breakdown (Doughnut)
        Map<String, Long> txnStatus = transactions.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getStatus() != null ? t.getStatus() : "UNKNOWN",
                        Collectors.counting()));
        charts.put("transactionStatus", txnStatus);

        // 2. Transaction Volume by Day (Last 7 days - Line)
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("MMM dd");
        Map<String, Long> dailyVolume = new LinkedHashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDateTime day = LocalDateTime.now().minusDays(i);
            dailyVolume.put(day.format(dayFmt), 0L);
        }
        transactions.stream()
                .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(sevenDaysAgo))
                .forEach(t -> {
                    String key = t.getTxnDate().format(dayFmt);
                    dailyVolume.computeIfPresent(key, (k, v) -> v + 1);
                });
        charts.put("dailyTransactionVolume", dailyVolume);

        // 3. Payment Channel Distribution (Doughnut)
        Map<String, Long> channelDist = transactions.stream()
                .filter(t -> t.getPaymentChannel() != null && !t.getPaymentChannel().isEmpty())
                .collect(Collectors.groupingBy(Transaction::getPaymentChannel, Collectors.counting()));
        charts.put("paymentChannelDistribution", channelDist);

        // 4. Settlement Type Breakdown (Doughnut)
        Map<String, Long> settlementTypes = settlements.stream()
                .filter(s -> s.getSettlementType() != null)
                .collect(Collectors.groupingBy(Settlement::getSettlementType, Collectors.counting()));
        charts.put("settlementTypes", settlementTypes);

        // 5. Transaction Amount by Currency (Bar)
        Map<String, Double> amountByCurrency = transactions.stream()
                .filter(t -> t.getCurrency() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getCurrency,
                        Collectors.summingDouble(t -> t.getAmount() != null ? t.getAmount().doubleValue() : 0)));
        charts.put("amountByCurrency", amountByCurrency);

        // 6. Top 5 Merchants by Transaction Volume (Bar) - Admin only
        if ("ADMIN".equals(currentUser.getRole())) {
            Map<String, Long> merchantVolume = transactions.stream()
                    .filter(t -> t.getMerchantName() != null)
                    .collect(Collectors.groupingBy(Transaction::getMerchantName, Collectors.counting()));
            Map<String, Long> topMerchants = merchantVolume.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a, LinkedHashMap::new));
            charts.put("topMerchants", topMerchants);

            // 7. Merchant Status Distribution (Doughnut) - Admin only
            Map<String, Long> merchantStatus = merchants.stream()
                    .collect(Collectors.groupingBy(
                            m -> m.getStatus() != null ? m.getStatus() : "UNKNOWN",
                            Collectors.counting()));
            charts.put("merchantStatus", merchantStatus);
        }

        return charts;
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
