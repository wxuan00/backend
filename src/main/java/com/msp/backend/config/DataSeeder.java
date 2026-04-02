package com.msp.backend.config;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.msp.backend.modules.merchant.Merchant;
import com.msp.backend.modules.merchant.MerchantRepository;
import com.msp.backend.modules.analytics.Analytics;
import com.msp.backend.modules.analytics.AnalyticsRepository;
import com.msp.backend.modules.role.Permission;
import com.msp.backend.modules.role.PermissionRepository;
import com.msp.backend.modules.role.Role;
import com.msp.backend.modules.role.RolePermission;
import com.msp.backend.modules.role.RolePermissionRepository;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.modules.settlement.CreditAdvice;
import com.msp.backend.modules.settlement.CreditAdviceRepository;
import com.msp.backend.modules.settlement.Settlement;
import com.msp.backend.modules.settlement.SettlementRepository;
import com.msp.backend.modules.transaction.Refund;
import com.msp.backend.modules.transaction.RefundRepository;
import com.msp.backend.modules.transaction.Transaction;
import com.msp.backend.modules.transaction.TransactionRepository;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserRole;
import com.msp.backend.modules.user.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;
    private final RefundRepository refundRepository;
    private final SettlementRepository settlementRepository;
    private final CreditAdviceRepository creditAdviceRepository;
    private final AnalyticsRepository analyticsRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    private final Random random = new Random();

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains data. Skipping seeding.");
            return;
        }
        seedAll();
    }

    public void reseed() {
        log.info("=== Clearing existing data ===");
        analyticsRepository.deleteAll();
        refundRepository.deleteAll();
        transactionRepository.deleteAll();
        settlementRepository.deleteAll();
        creditAdviceRepository.deleteAll();
        userRoleRepository.deleteAll();
        rolePermissionRepository.deleteAll();
        permissionRepository.deleteAll();
        userRepository.deleteAll();
        merchantRepository.deleteAll();
        roleRepository.deleteAll();
        seedAll();
    }

    private void seedAll() {
        log.info("=== Starting Database Seeding ===");

        List<Role> roles = seedRoles();
        List<Permission> permissions = seedPermissions();
        seedRolePermissions(roles, permissions);
        List<Merchant> merchants = seedMerchants();
        List<User> users = seedUsers(roles);
        linkMerchantsToUsers(merchants, users);
        List<Transaction> transactions = seedTransactions(merchants);
        List<CreditAdvice> creditAdvices = seedCreditAdvices(merchants);
        seedSettlements(creditAdvices, transactions);
        seedRefunds(transactions, merchants);
        seedAnalytics(merchants, transactions);

        log.info("=== Database Seeding Complete ===");
        log.info("Login credentials:");
        log.info("  Admin: admin@msp.com / admin123");
        log.info("  Merchant: john.tech@example.com / merchant123");
    }

    // ── JSON helpers ──────────────────────────────────────────

    private <T> List<T> loadJsonList(String path, TypeReference<List<T>> typeRef) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(is, typeRef);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load seed data from " + path, e);
        }
    }

    private JsonNode loadJsonNode(String path) {
        try (InputStream is = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readTree(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load seed data from " + path, e);
        }
    }

    // ── Static data from JSON ─────────────────────────────────

    private List<Role> seedRoles() {
        log.info("Seeding roles...");
        List<Map<String, String>> data = loadJsonList(
                "seed-data/roles.json", new TypeReference<>() {});

        for (Map<String, String> d : data) {
            Role role = new Role();
            role.setRoleName(d.get("roleName"));
            role.setDescription(d.get("description"));
            role.setRoleType(d.get("roleType"));
            roleRepository.save(role);
        }

        List<Role> roles = roleRepository.findAll();
        log.info("Created {} roles", roles.size());
        return roles;
    }

    private List<Permission> seedPermissions() {
        log.info("Seeding permissions...");
        List<Map<String, String>> data = loadJsonList(
                "seed-data/permissions.json", new TypeReference<>() {});

        List<Permission> permissions = new ArrayList<>();
        for (Map<String, String> d : data) {
            Permission perm = new Permission();
            perm.setPermissionName(d.get("permissionName"));
            perm.setDescription(d.get("description"));
            perm.setModule(d.get("module"));
            permissions.add(permissionRepository.save(perm));
        }

        log.info("Created {} permissions", permissions.size());
        return permissions;
    }

    private void seedRolePermissions(List<Role> roles, List<Permission> permissions) {
        log.info("Seeding role-permissions...");
        JsonNode mapping = loadJsonNode("seed-data/role-permissions.json");

        mapping.properties().forEach(entry -> {
            String roleName = entry.getKey();
            Role role = roles.stream()
                    .filter(r -> roleName.equals(r.getRoleName()))
                    .findFirst().orElseThrow();

            List<String> permNames = new ArrayList<>();
            entry.getValue().forEach(node -> permNames.add(node.textValue()));

            boolean allPerms = permNames.contains("ALL");
            List<Permission> toAssign = allPerms
                    ? permissions
                    : permissions.stream()
                        .filter(p -> permNames.contains(p.getPermissionName()))
                        .toList();

            for (Permission p : toAssign) {
                RolePermission rp = new RolePermission();
                rp.setRoleId(role.getRoleId());
                rp.setPermissionId(p.getPermissionId());
                rp.setGeneratedBy("SYSTEM");
                rp.setGeneratedAt(LocalDateTime.now());
                rp.setLastModifiedBy("SYSTEM");
                rp.setLastModifiedAt(LocalDateTime.now());
                rolePermissionRepository.save(rp);
            }
        });

        log.info("Role permissions assigned");
    }

    private List<Merchant> seedMerchants() {
        log.info("Seeding merchants...");
        List<Map<String, String>> data = loadJsonList(
                "seed-data/merchants.json", new TypeReference<>() {});

        for (Map<String, String> d : data) {
            Merchant m = new Merchant();
            m.setMerchantName(d.get("merchantName"));
            m.setContact(d.get("contact"));
            m.setAddressLine1(d.get("addressLine1"));
            m.setAddressLine2(d.get("addressLine2"));
            m.setPostcode(d.get("postcode"));
            m.setCity(d.get("city"));
            m.setCountry(d.get("country"));
            m.setStatus(d.get("status"));
            merchantRepository.save(m);
        }

        List<Merchant> merchants = merchantRepository.findAll();
        log.info("Created {} merchants", merchants.size());
        return merchants;
    }

    private List<User> seedUsers(List<Role> roles) {
        log.info("Seeding users...");
        List<JsonNode> data = loadJsonList(
                "seed-data/users.json", new TypeReference<>() {});

        List<User> users = new ArrayList<>();
        for (JsonNode d : data) {
            User u = new User();
            u.setEmail(d.get("email").textValue());
            u.setPassword(passwordEncoder.encode(d.get("password").textValue()));
            u.setFirstName(d.get("firstName").textValue());
            u.setLastName(d.get("lastName").textValue());
            u.setDisplayName(d.get("displayName").textValue());
            u.setContactNumber(d.get("contactNumber").textValue());
            u.setStatus(d.get("status").textValue());
            u.setMfaEnabled(d.get("mfaEnabled").booleanValue());
            u.setMustChangePassword(d.get("mustChangePassword").booleanValue());

            User saved = userRepository.save(u);
            users.add(saved);

            // Assign role from JSON
            String roleName = d.get("role").textValue();
            Role role = roles.stream()
                    .filter(r -> roleName.equals(r.getRoleName()))
                    .findFirst().orElseThrow();

            UserRole ur = new UserRole();
            ur.setUserId(saved.getUserId());
            ur.setRoleId(role.getRoleId());
            ur.setGeneratedBy("SYSTEM");
            ur.setGeneratedAt(LocalDateTime.now());
            ur.setLastModifiedBy("SYSTEM");
            ur.setLastModifiedAt(LocalDateTime.now());
            userRoleRepository.save(ur);
        }

        log.info("Created {} users", users.size());
        return users;
    }

    // ── Dynamic data (generated) ──────────────────────────────

    private void linkMerchantsToUsers(List<Merchant> merchants, List<User> users) {
        log.info("Linking merchants to users...");
        List<User> merchantUsers = users.stream()
                .filter(u -> !"admin@msp.com".equals(u.getEmail())
                           && !"manager@msp.com".equals(u.getEmail()))
                .toList();

        for (int i = 0; i < merchants.size() && i < merchantUsers.size(); i++) {
            Merchant m = merchants.get(i);
            m.setUserId(merchantUsers.get(i).getUserId());
            merchantRepository.save(m);
        }
        log.info("Merchants linked to users");
    }

    private List<Transaction> seedTransactions(List<Merchant> merchants) {
        log.info("Seeding transactions...");
        List<JsonNode> data = loadJsonList(
                "seed-data/transactions.json", new TypeReference<>() {});

        List<Transaction> allTransactions = new ArrayList<>();
        for (int i = 0; i < data.size(); i++) {
            JsonNode d = data.get(i);
            Merchant merchant = merchants.get(i % merchants.size());

            Transaction t = new Transaction();
            t.setMerchantId(merchant.getMerchantId());
            t.setRefNo(d.get("refNo").textValue());
            t.setCardNo(d.get("cardNo").textValue());
            t.setTxnDescription(d.get("txnDescription").textValue());
            t.setAmount(new BigDecimal(d.get("amount").textValue()));
            t.setDiscountAmount(BigDecimal.ZERO);
            t.setNettAmount(new BigDecimal(d.get("amount").textValue()));
            t.setCurrency(d.get("currency").textValue());
            t.setStatus(d.get("status").textValue());
            t.setPaymentChannel(d.get("paymentChannel").textValue());
            t.setTxnDate(LocalDateTime.parse(d.get("txnDate").textValue()));

            if ("APPROVED".equals(t.getStatus())) {
                t.setPostedDate(t.getTxnDate().plusDays(1 + random.nextInt(3)));
            }

            allTransactions.add(transactionRepository.save(t));
        }

        log.info("Created {} transactions", allTransactions.size());
        return allTransactions;
    }

    private void seedRefunds(List<Transaction> transactions, List<Merchant> merchants) {
        log.info("Seeding refunds...");
        List<JsonNode> data = loadJsonList(
                "seed-data/refunds.json", new TypeReference<>() {});

        int refundCount = 0;
        for (JsonNode d : data) {
            int txnIdx = d.get("transactionIndex").intValue();
            if (txnIdx >= transactions.size()) continue;

            Transaction t = transactions.get(txnIdx);

            Refund r = new Refund();
            r.setTransactionId(t.getTransactionId());
            r.setMerchantId(t.getMerchantId());
            r.setCardNo(t.getCardNo());
            r.setCurrency(t.getCurrency());
            r.setAmount(t.getAmount());

            String refundType = d.get("refundType").textValue();
            r.setRefundType(refundType);

            if ("FULL".equals(refundType)) {
                r.setRefundAmount(t.getAmount());
            } else {
                int pct = d.has("refundPercent") ? d.get("refundPercent").intValue() : 50;
                BigDecimal refundAmount = t.getAmount()
                        .multiply(BigDecimal.valueOf(pct))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                r.setRefundAmount(refundAmount);
            }

            r.setRefundRefNo(d.get("refundRefNo").textValue());
            r.setStatus(d.get("status").textValue());
            r.setTransactionDate(t.getTxnDate());
            r.setSubmissionDate(LocalDateTime.parse(d.get("submissionDate").textValue()));

            if ("APPROVED".equals(r.getStatus()) && d.has("postedDate")) {
                r.setPostedDate(LocalDateTime.parse(d.get("postedDate").textValue()));
            }

            refundRepository.save(r);
            refundCount++;
        }

        log.info("Created {} refunds", refundCount);
    }

    private List<CreditAdvice> seedCreditAdvices(List<Merchant> merchants) {
        log.info("Seeding credit advices...");
        List<JsonNode> data = loadJsonList(
                "seed-data/credit-advices.json", new TypeReference<>() {});

        List<CreditAdvice> allAdvices = new ArrayList<>();
        for (JsonNode d : data) {
            int merchantIdx = d.get("merchantIndex").intValue();
            Merchant merchant = merchants.get(merchantIdx);

            CreditAdvice ca = new CreditAdvice();
            ca.setMerchantId(merchant.getMerchantId());
            ca.setAccountNo(d.get("accountNo").textValue());
            ca.setAccountId(d.get("accountId").textValue());
            ca.setCurrency(d.get("currency").textValue());
            ca.setAmount(new BigDecimal(d.get("amount").textValue()));
            ca.setPaymentDate(LocalDateTime.parse(d.get("paymentDate").textValue()));

            allAdvices.add(creditAdviceRepository.save(ca));
        }

        log.info("Created {} credit advices", allAdvices.size());
        return allAdvices;
    }

    private void seedSettlements(List<CreditAdvice> creditAdvices, List<Transaction> allTransactions) {
        log.info("Seeding settlements...");
        List<JsonNode> data = loadJsonList(
                "seed-data/settlements.json", new TypeReference<>() {});

        int settlementCount = 0;
        for (JsonNode d : data) {
            int caIdx = d.get("creditAdviceIndex").intValue();
            CreditAdvice ca = creditAdvices.get(caIdx);

            Settlement s = new Settlement();
            s.setCreditAdviceId(ca.getCreditAdviceId());
            s.setSettlementNo(d.get("settlementNo").textValue());
            s.setSettlementType(d.get("settlementType").textValue());
            s.setCurrency(d.get("currency").textValue());
            s.setSettlementDate(LocalDateTime.parse(d.get("settlementDate").textValue()));

            // Collect referenced transactions
            List<Transaction> settlementTxns = new ArrayList<>();
            JsonNode txnIndexes = d.get("transactionIndexes");
            if (txnIndexes != null && txnIndexes.isArray()) {
                for (JsonNode idx : txnIndexes) {
                    int txnIdx = idx.intValue();
                    if (txnIdx < allTransactions.size()) {
                        settlementTxns.add(allTransactions.get(txnIdx));
                    }
                }
            }

            if (!settlementTxns.isEmpty()) {
                BigDecimal settlementAmount = settlementTxns.stream()
                        .map(t -> t.getNettAmount() != null ? t.getNettAmount() : t.getAmount())
                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        .setScale(2, RoundingMode.HALF_UP);
                s.setSettlementAmount(settlementAmount);
                s.setPaymentAmount(settlementAmount.multiply(BigDecimal.valueOf(0.975))
                        .setScale(2, RoundingMode.HALF_UP));
            } else {
                s.setSettlementAmount(ca.getAmount());
                s.setPaymentAmount(ca.getAmount().multiply(BigDecimal.valueOf(0.975))
                        .setScale(2, RoundingMode.HALF_UP));
            }

            Settlement saved = settlementRepository.save(s);
            settlementCount++;

            // Link transactions back to this settlement
            for (Transaction t : settlementTxns) {
                t.setSettlementId(saved.getSettlementId());
                transactionRepository.save(t);
            }
        }

        log.info("Created {} settlements", settlementCount);
    }

    private void seedAnalytics(List<Merchant> merchants, List<Transaction> transactions) {
        log.info("Seeding analytics records...");
        int count = 0;
        String[] metrics = {"TOTAL_SALES", "TOTAL_TRANSACTIONS", "DECLINE_RATE", "AVG_TRANSACTION", "NET_REVENUE", "REFUND_RATE"};

        for (Merchant merchant : merchants) {
            List<Transaction> mTxns = transactions.stream()
                    .filter(t -> merchant.getMerchantId().equals(t.getMerchantId()))
                    .toList();

            long approved = mTxns.stream().filter(t -> "APPROVED".equals(t.getStatus())).count();
            long declined = mTxns.stream().filter(t -> "DECLINED".equals(t.getStatus())).count();
            BigDecimal totalSales = mTxns.stream()
                    .filter(t -> "APPROVED".equals(t.getStatus()) && t.getAmount() != null)
                    .map(Transaction::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal avgTxn = approved > 0 ? totalSales.divide(BigDecimal.valueOf(approved), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            double declineRate = mTxns.isEmpty() ? 0 : (double) declined / mTxns.size() * 100;

            String[] values = {
                    totalSales.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    String.valueOf(mTxns.size()),
                    String.format("%.2f", declineRate),
                    avgTxn.toPlainString(),
                    totalSales.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                    String.format("%.2f", random.nextDouble() * 10)
            };

            for (int i = 0; i < metrics.length; i++) {
                Analytics a = new Analytics();
                a.setMerchantId(merchant.getMerchantId());
                a.setDataName(metrics[i]);
                a.setDataValue(values[i]);
                a.setGeneratedAt(LocalDateTime.now().minusHours(random.nextInt(48)));
                analyticsRepository.save(a);
                count++;
            }
        }
        log.info("Created {} analytics records", count);
    }
}
