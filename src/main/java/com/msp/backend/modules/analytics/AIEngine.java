package com.msp.backend.modules.analytics;

import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.settlement.Settlement;
import com.msp.backend.modules.transaction.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * AI Analytics Engine - Generates smart insights from transaction,
 * merchant, and settlement data using rule-based heuristics.
 */
@Component
public class AIEngine {

    public List<Map<String, Object>> generateInsights(
            List<Transaction> transactions,
            List<Merchant> merchants,
            List<Settlement> settlements
    ) {
        List<Map<String, Object>> insights = new ArrayList<>();

        analyzeTransactionVolume(transactions, insights);
        analyzeDeclineRate(transactions, insights);
        analyzeTopMerchants(transactions, insights);
        analyzeSettlementSummary(settlements, insights);
        analyzePaymentChannels(transactions, insights);
        analyzeMerchantRisk(transactions, merchants, insights);
        analyzeRevenue(transactions, insights);

        return insights;
    }

    public List<Map<String, Object>> generateMerchantInsights(
            List<Transaction> transactions,
            List<Settlement> settlements
    ) {
        List<Map<String, Object>> insights = new ArrayList<>();

        analyzeTransactionVolume(transactions, insights);
        analyzeDeclineRate(transactions, insights);
        analyzePaymentChannels(transactions, insights);
        analyzeSettlementSummary(settlements, insights);
        analyzeRevenue(transactions, insights);

        return insights;
    }

    // ----- Individual analysis methods -----

    private void analyzeTransactionVolume(List<Transaction> transactions, List<Map<String, Object>> insights) {
        if (transactions.isEmpty()) return;

        long total = transactions.size();
        LocalDateTime now = LocalDateTime.now();
        long last7Days = transactions.stream()
                .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(7)))
                .count();
        long last30Days = transactions.stream()
                .filter(t -> t.getTxnDate() != null && t.getTxnDate().isAfter(now.minusDays(30)))
                .count();

        String trend;
        String severity;
        if (last7Days > 0 && last30Days > 0) {
            double weeklyRate = (double) last7Days / 7;
            double monthlyRate = (double) last30Days / 30;
            double change = ((weeklyRate - monthlyRate) / monthlyRate) * 100;

            if (change > 20) {
                trend = String.format("Transaction volume is UP %.0f%% this week vs monthly average (%.0f/day vs %.0f/day). Growth is accelerating.", change, weeklyRate, monthlyRate);
                severity = "success";
            } else if (change < -20) {
                trend = String.format("Transaction volume is DOWN %.0f%% this week vs monthly average. Consider investigating.", Math.abs(change));
                severity = "warning";
            } else {
                trend = String.format("Transaction volume is STABLE with ~%.0f transactions/day average.", monthlyRate);
                severity = "info";
            }
        } else {
            trend = String.format("Total of %d transactions recorded.", total);
            severity = "info";
        }

        insights.add(createInsight("Transaction Volume", trend, severity, "volume"));
    }

    private void analyzeDeclineRate(List<Transaction> transactions, List<Map<String, Object>> insights) {
        if (transactions.isEmpty()) return;

        long total = transactions.size();
        long declined = transactions.stream().filter(t -> "DECLINED".equals(t.getStatus())).count();
        double declineRate = (double) declined / total * 100;

        String message;
        String severity;
        if (declineRate > 10) {
            message = String.format("Decline rate is HIGH at %.1f%% (%d of %d transactions). This may indicate issues with payment processing or fraud risk.", declineRate, declined, total);
            severity = "danger";
        } else if (declineRate > 5) {
            message = String.format("Decline rate is MODERATE at %.1f%% (%d declined). Monitor for any increasing trends.", declineRate, declined);
            severity = "warning";
        } else {
            message = String.format("Decline rate is HEALTHY at %.1f%% (%d of %d transactions). Well within acceptable range.", declineRate, declined, total);
            severity = "success";
        }

        insights.add(createInsight("Decline Rate Analysis", message, severity, "decline"));
    }

    private void analyzeTopMerchants(List<Transaction> transactions, List<Map<String, Object>> insights) {
        if (transactions.isEmpty()) return;

        Map<String, Long> merchantCounts = transactions.stream()
                .filter(t -> t.getMerchantName() != null)
                .collect(Collectors.groupingBy(Transaction::getMerchantName, Collectors.counting()));

        List<Map.Entry<String, Long>> top = merchantCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(3)
                .toList();

        if (!top.isEmpty()) {
            StringBuilder sb = new StringBuilder("Top merchants by transaction count: ");
            for (int i = 0; i < top.size(); i++) {
                Map.Entry<String, Long> entry = top.get(i);
                sb.append(String.format("%d) %s (%d txns)", i + 1, entry.getKey(), entry.getValue()));
                if (i < top.size() - 1) sb.append(", ");
            }
            insights.add(createInsight("Top Merchants", sb.toString(), "info", "top-merchants"));
        }
    }

    private void analyzeSettlementSummary(List<Settlement> settlements, List<Map<String, Object>> insights) {
        if (settlements.isEmpty()) return;

        long total = settlements.size();

        Map<String, Long> byType = settlements.stream()
                .filter(s -> s.getSettlementType() != null)
                .collect(Collectors.groupingBy(Settlement::getSettlementType, Collectors.counting()));

        BigDecimal totalAmount = settlements.stream()
                .map(Settlement::getSettlementAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Total %d settlements worth %s. ", total, totalAmount.setScale(2, RoundingMode.HALF_UP).toPlainString()));

        if (!byType.isEmpty()) {
            sb.append("Breakdown by type: ");
            byType.forEach((type, count) -> sb.append(String.format("%s: %d, ", type, count)));
        }

        String message = sb.toString().replaceAll(", $", "");
        insights.add(createInsight("Settlement Summary", message, "info", "settlement"));
    }

    private void analyzePaymentChannels(List<Transaction> transactions, List<Map<String, Object>> insights) {
        if (transactions.isEmpty()) return;

        Map<String, Long> channelCounts = transactions.stream()
                .filter(t -> t.getPaymentChannel() != null && !t.getPaymentChannel().isEmpty())
                .collect(Collectors.groupingBy(Transaction::getPaymentChannel, Collectors.counting()));

        if (channelCounts.isEmpty()) return;

        long total = channelCounts.values().stream().mapToLong(Long::longValue).sum();
        StringBuilder sb = new StringBuilder("Payment channel distribution: ");
        channelCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(e -> sb.append(String.format("%s %.0f%%, ", e.getKey(), (double) e.getValue() / total * 100)));

        insights.add(createInsight("Payment Channels", sb.toString().replaceAll(", $", ""), "info", "channels"));
    }

    private void analyzeMerchantRisk(List<Transaction> transactions, List<Merchant> merchants, List<Map<String, Object>> insights) {
        if (transactions.isEmpty() || merchants.isEmpty()) return;

        Map<Long, List<Transaction>> byMerchant = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getMerchantId));

        List<String> riskyMerchants = new ArrayList<>();
        for (Map.Entry<Long, List<Transaction>> entry : byMerchant.entrySet()) {
            List<Transaction> txns = entry.getValue();
            long declined = txns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count();
            double rate = (double) declined / txns.size() * 100;
            if (rate > 15 && txns.size() >= 5) {
                String name = txns.get(0).getMerchantName();
                riskyMerchants.add(String.format("%s (%.0f%% decline)", name != null ? name : "ID:" + entry.getKey(), rate));
            }
        }

        if (!riskyMerchants.isEmpty()) {
            String message = "High-risk merchants detected with decline rates >15%: " + String.join(", ", riskyMerchants) + ". Consider reviewing their accounts.";
            insights.add(createInsight("Risk Alert", message, "danger", "risk"));
        } else {
            insights.add(createInsight("Risk Assessment", "No merchants currently flagged as high-risk. All decline rates are within acceptable limits.", "success", "risk"));
        }
    }

    private void analyzeRevenue(List<Transaction> transactions, List<Map<String, Object>> insights) {
        if (transactions.isEmpty()) return;

        BigDecimal totalApproved = transactions.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getAmount() != null)
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalNett = transactions.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getNettAmount() != null)
                .map(Transaction::getNettAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscount = transactions.stream()
                .filter(t -> "APPROVED".equals(t.getStatus()) && t.getDiscountAmount() != null)
                .map(Transaction::getDiscountAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String message = String.format(
                "Total approved amount: MYR %s | Total discount: MYR %s | Net amount: MYR %s",
                totalApproved.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                totalDiscount.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                totalNett.setScale(2, RoundingMode.HALF_UP).toPlainString()
        );

        insights.add(createInsight("Revenue Summary", message, "info", "revenue"));
    }

    private Map<String, Object> createInsight(String title, String message, String severity, String category) {
        Map<String, Object> insight = new HashMap<>();
        insight.put("title", title);
        insight.put("message", message);
        insight.put("severity", severity);
        insight.put("category", category);
        insight.put("generatedAt", LocalDateTime.now().toString());
        return insight;
    }
}
