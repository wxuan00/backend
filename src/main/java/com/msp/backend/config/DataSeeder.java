package com.msp.backend.config;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
        List<User> users = seedUsers();
        seedUserRoles(users, roles, merchants);
        linkMerchantsToUsers(merchants, users);
        List<Transaction> transactions = seedTransactions(merchants);
        seedRefunds(transactions, merchants);
        List<CreditAdvice> creditAdvices = seedCreditAdvices(merchants);
        seedSettlements(creditAdvices);
        seedAnalytics(merchants, transactions);

        log.info("=== Database Seeding Complete ===");
        log.info("Login credentials:");
        log.info("  Admin: admin@msp.com / admin123");
        log.info("  Merchant: john.tech@example.com / merchant123");
    }

    private List<Role> seedRoles() {
        log.info("Seeding roles...");

        Role admin = new Role();
        admin.setRoleName("ADMIN");
        admin.setDescription("System administrator with full access");
        admin.setRoleType("SYSTEM");
        roleRepository.save(admin);

        Role merchant = new Role();
        merchant.setRoleName("MERCHANT");
        merchant.setDescription("Merchant user with limited access to own data");
        merchant.setRoleType("BUSINESS");
        roleRepository.save(merchant);

        Role viewer = new Role();
        viewer.setRoleName("VIEWER");
        viewer.setDescription("Read-only access to reports");
        viewer.setRoleType("SYSTEM");
        roleRepository.save(viewer);

        log.info("Created 3 roles");
        return roleRepository.findAll();
    }

    private List<Permission> seedPermissions() {
        log.info("Seeding permissions...");
        String[][] permData = {
            {"VIEW_DASHBOARD", "View dashboard and analytics", "DASHBOARD"},
            {"MANAGE_USERS", "Create, update, delete users", "USER"},
            {"MANAGE_ROLES", "Create, update, delete roles", "ROLE"},
            {"MANAGE_MERCHANTS", "Create, update, delete merchants", "MERCHANT"},
            {"VIEW_TRANSACTIONS", "View transaction records", "TRANSACTION"},
            {"VIEW_SETTLEMENTS", "View settlement records", "SETTLEMENT"},
            {"VIEW_CREDIT_ADVICES", "View credit advice records", "CREDIT_ADVICE"},
            {"VIEW_REFUNDS", "View refund records", "REFUND"},
            {"GENERATE_REPORTS", "Generate and export reports", "REPORT"},
            {"VIEW_OWN_DATA", "View own merchant data", "MERCHANT"}
        };
        List<Permission> permissions = new ArrayList<>();
        for (String[] p : permData) {
            Permission perm = new Permission();
            perm.setPermissionName(p[0]);
            perm.setDescription(p[1]);
            perm.setModule(p[2]);
            permissions.add(permissionRepository.save(perm));
        }
        log.info("Created {} permissions", permissions.size());
        return permissions;
    }

    private void seedRolePermissions(List<Role> roles, List<Permission> permissions) {
        log.info("Seeding role-permissions...");
        Role adminRole = roles.stream().filter(r -> "ADMIN".equals(r.getRoleName())).findFirst().orElseThrow();
        Role merchantRole = roles.stream().filter(r -> "MERCHANT".equals(r.getRoleName())).findFirst().orElseThrow();

        // Admin gets all permissions
        for (Permission p : permissions) {
            RolePermission rp = new RolePermission();
            rp.setRoleId(adminRole.getRoleId());
            rp.setPermissionId(p.getPermissionId());
            rp.setGeneratedBy("SYSTEM");
            rp.setGeneratedAt(LocalDateTime.now());
            rp.setLastModifiedBy("SYSTEM");
            rp.setLastModifiedAt(LocalDateTime.now());
            rolePermissionRepository.save(rp);
        }

        // Merchant gets limited permissions
        String[] merchantPerms = {"VIEW_DASHBOARD", "VIEW_TRANSACTIONS", "VIEW_SETTLEMENTS", "VIEW_CREDIT_ADVICES", "VIEW_REFUNDS", "VIEW_OWN_DATA"};
        for (String permName : merchantPerms) {
            permissions.stream().filter(p -> permName.equals(p.getPermissionName())).findFirst().ifPresent(p -> {
                RolePermission rp = new RolePermission();
                rp.setRoleId(merchantRole.getRoleId());
                rp.setPermissionId(p.getPermissionId());
                rp.setGeneratedBy("SYSTEM");
                rp.setGeneratedAt(LocalDateTime.now());
                rp.setLastModifiedBy("SYSTEM");
                rp.setLastModifiedAt(LocalDateTime.now());
                rolePermissionRepository.save(rp);
            });
        }

        log.info("Role permissions assigned");
    }

    private List<Merchant> seedMerchants() {
        log.info("Seeding merchants...");
        String[][] merchantData = {
            {"TechMart Solutions", "+60123456789", "123 Tech Street", "Level 2", "50100", "Kuala Lumpur", "Malaysia", "ACTIVE"},
            {"FoodHub Malaysia", "+60198765432", "456 Food Avenue", "Ground Floor", "47301", "Petaling Jaya", "Malaysia", "ACTIVE"},
            {"Fashion Forward", "+60112233445", "789 Style Mall", "Unit 3-01", "80000", "Johor Bahru", "Malaysia", "ACTIVE"},
            {"GreenGrocers Sdn Bhd", "+60187654321", "321 Fresh Market", "Lot 12", "10200", "Georgetown", "Malaysia", "ACTIVE"},
            {"AutoParts Plus", "+60145678901", "555 Motor Lane", "Block B", "75000", "Melaka", "Malaysia", "ACTIVE"},
            {"BookWorld Malaysia", "+60156789012", "888 Knowledge St", "Floor 1", "50450", "Kuala Lumpur", "Malaysia", "ACTIVE"},
            {"HealthFirst Pharmacy", "+60167890123", "222 Wellness Rd", "Unit A", "40170", "Shah Alam", "Malaysia", "ACTIVE"},
            {"Digital Dreams", "+60178901234", "444 Tech Hub", "Suite 5", "63000", "Cyberjaya", "Malaysia", "PENDING"},
            {"PetCare Express", "+60189012345", "666 Animal Ave", "Shop 8", "53300", "Kuala Lumpur", "Malaysia", "SUSPENDED"},
            {"HomeStyle Furniture", "+60190123456", "777 Comfort Blvd", "Warehouse 2", "40000", "Shah Alam", "Malaysia", "ACTIVE"}
        };

        for (String[] d : merchantData) {
            Merchant m = new Merchant();
            m.setMerchantName(d[0]);
            m.setContact(d[1]);
            m.setAddressLine1(d[2]);
            m.setAddressLine2(d[3]);
            m.setPostcode(d[4]);
            m.setCity(d[5]);
            m.setCountry(d[6]);
            m.setStatus(d[7]);
            merchantRepository.save(m);
        }

        log.info("Created {} merchants", merchantData.length);
        return merchantRepository.findAll();
    }

    private List<User> seedUsers() {
        log.info("Seeding users...");
        List<User> users = new ArrayList<>();

        // Admin user
        User admin = new User();
        admin.setEmail("admin@msp.com");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setFirstName("System");
        admin.setLastName("Administrator");
        admin.setDisplayName("Admin");
        admin.setContactNumber("+60123456000");
        admin.setStatus("ACTIVE");
        admin.setMfaEnabled(false);
        users.add(userRepository.save(admin));

        // Another admin
        User admin2 = new User();
        admin2.setEmail("manager@msp.com");
        admin2.setPassword(passwordEncoder.encode("manager123"));
        admin2.setFirstName("Sarah");
        admin2.setLastName("Manager");
        admin2.setDisplayName("Sarah M");
        admin2.setContactNumber("+60123456001");
        admin2.setStatus("ACTIVE");
        admin2.setMfaEnabled(false);
        users.add(userRepository.save(admin2));

        // Merchant users
        String[][] merchantUsers = {
            {"john.tech@example.com", "John", "Tan", "John T"},
            {"mary.food@example.com", "Mary", "Lim", "Mary L"},
            {"alex.fashion@example.com", "Alex", "Wong", "Alex W"},
            {"david.green@example.com", "David", "Lee", "David L"},
            {"lisa.auto@example.com", "Lisa", "Chen", "Lisa C"},
            {"peter.book@example.com", "Peter", "Ng", "Peter N"},
            {"karen.health@example.com", "Karen", "Ong", "Karen O"},
            {"mike.digital@example.com", "Mike", "Yap", "Mike Y"},
            {"jenny.pet@example.com", "Jenny", "Koh", "Jenny K"},
            {"robert.home@example.com", "Robert", "Teo", "Robert T"}
        };

        for (int i = 0; i < merchantUsers.length; i++) {
            User u = new User();
            u.setEmail(merchantUsers[i][0]);
            u.setPassword(passwordEncoder.encode("merchant123"));
            u.setFirstName(merchantUsers[i][1]);
            u.setLastName(merchantUsers[i][2]);
            u.setDisplayName(merchantUsers[i][3]);
            u.setContactNumber("+6012345" + String.format("%04d", i + 10));
            u.setStatus("ACTIVE");
            u.setMfaEnabled(false);
            users.add(userRepository.save(u));
        }

        log.info("Created {} users", users.size());
        return users;
    }

    private void seedUserRoles(List<User> users, List<Role> roles, List<Merchant> merchants) {
        log.info("Seeding user-roles...");
        Role adminRole = roles.stream().filter(r -> "ADMIN".equals(r.getRoleName())).findFirst().orElseThrow();
        Role merchantRole = roles.stream().filter(r -> "MERCHANT".equals(r.getRoleName())).findFirst().orElseThrow();

        for (User user : users) {
            UserRole ur = new UserRole();
            ur.setUserId(user.getUserId());

            // First two users are admins, rest are merchants
            if ("admin@msp.com".equals(user.getEmail()) || "manager@msp.com".equals(user.getEmail())) {
                ur.setRoleId(adminRole.getRoleId());
            } else {
                ur.setRoleId(merchantRole.getRoleId());
            }

            ur.setGeneratedBy("SYSTEM");
            ur.setGeneratedAt(LocalDateTime.now());
            ur.setLastModifiedBy("SYSTEM");
            ur.setLastModifiedAt(LocalDateTime.now());
            userRoleRepository.save(ur);
        }

        log.info("User roles assigned");
    }

    private void linkMerchantsToUsers(List<Merchant> merchants, List<User> users) {
        log.info("Linking merchants to users...");
        // Skip first 2 users (admins), link merchant users (index 2+) to merchants
        for (int i = 0; i < merchants.size() && (i + 2) < users.size(); i++) {
            Merchant m = merchants.get(i);
            m.setUserId(users.get(i + 2).getUserId());
            merchantRepository.save(m);
        }
        log.info("Merchants linked to users");
    }

    private List<Transaction> seedTransactions(List<Merchant> merchants) {
        log.info("Seeding transactions...");

        String[] statuses = {"APPROVED", "APPROVED", "APPROVED", "APPROVED", "PENDING", "DECLINED"};
        String[] channels = {"CARD", "CARD", "CARD", "E_WALLET", "ONLINE_BANKING", "QR_PAY"};
        List<Transaction> allTransactions = new ArrayList<>();

        for (Merchant merchant : merchants) {
            int numTransactions = 20 + random.nextInt(21);

            for (int i = 0; i < numTransactions; i++) {
                Transaction t = new Transaction();
                t.setMerchantId(merchant.getMerchantId());
                t.setPaymentChannel(channels[random.nextInt(channels.length)]);

                BigDecimal amount = BigDecimal.valueOf(10 + random.nextDouble() * 990).setScale(2, RoundingMode.HALF_UP);
                BigDecimal discount = amount.multiply(BigDecimal.valueOf(random.nextDouble() * 0.05)).setScale(2, RoundingMode.HALF_UP);
                BigDecimal nett = amount.subtract(discount);

                t.setAmount(amount);
                t.setDiscountAmount(discount);
                t.setNettAmount(nett);
                t.setCurrency("MYR");
                t.setStatus(statuses[random.nextInt(statuses.length)]);
                t.setRefNo("REF" + String.format("%08d", random.nextInt(100000000)));
                t.setCardNo("****" + String.format("%04d", random.nextInt(10000)));
                t.setTxnDescription("Purchase at " + merchant.getMerchantName());

                LocalDateTime txnDate = LocalDateTime.now().minusDays(random.nextInt(90)).minusHours(random.nextInt(24));
                t.setTxnDate(txnDate);

                if ("APPROVED".equals(t.getStatus())) {
                    t.setPostedDate(txnDate.plusDays(1 + random.nextInt(3)));
                }

                allTransactions.add(transactionRepository.save(t));
            }
        }

        log.info("Created {} transactions", allTransactions.size());
        return allTransactions;
    }

    private void seedRefunds(List<Transaction> transactions, List<Merchant> merchants) {
        log.info("Seeding refunds...");
        int refundCount = 0;

        // Create refunds for ~10% of approved transactions
        for (Transaction t : transactions) {
            if ("APPROVED".equals(t.getStatus()) && random.nextInt(10) == 0) {
                Refund r = new Refund();
                r.setTransactionId(t.getTransactionId());
                r.setMerchantId(t.getMerchantId());
                r.setCardNo(t.getCardNo());
                r.setCurrency(t.getCurrency());
                r.setAmount(t.getAmount());

                String refundType = random.nextBoolean() ? "FULL" : "PARTIAL";
                r.setRefundType(refundType);
                BigDecimal refundAmount;
                if ("FULL".equals(refundType)) {
                    // Full refund: same as original amount
                    refundAmount = t.getAmount();
                } else {
                    // Partial refund: between 10% and 99% of original amount
                    double pct = 0.10 + random.nextDouble() * 0.89;
                    refundAmount = t.getAmount().multiply(BigDecimal.valueOf(pct)).setScale(2, RoundingMode.HALF_UP);
                    // Ensure partial is strictly less than original
                    if (refundAmount.compareTo(t.getAmount()) >= 0) {
                        refundAmount = t.getAmount().subtract(BigDecimal.valueOf(0.01));
                    }
                }
                r.setRefundAmount(refundAmount);
                r.setRefundRefNo("RFN" + String.format("%08d", random.nextInt(100000000)));
                r.setStatus(random.nextBoolean() ? "APPROVED" : "PENDING");

                LocalDateTime submDate = t.getTxnDate().plusDays(1 + random.nextInt(14));
                r.setSubmissionDate(submDate);
                r.setTransactionDate(t.getTxnDate());
                if ("APPROVED".equals(r.getStatus())) {
                    r.setPostedDate(submDate.plusDays(1 + random.nextInt(5)));
                }

                refundRepository.save(r);
                refundCount++;
            }
        }

        log.info("Created {} refunds", refundCount);
    }

    private List<CreditAdvice> seedCreditAdvices(List<Merchant> merchants) {
        log.info("Seeding credit advices...");
        List<CreditAdvice> allAdvices = new ArrayList<>();

        for (Merchant merchant : merchants) {
            int numAdvices = 2 + random.nextInt(4);
            for (int i = 0; i < numAdvices; i++) {
                CreditAdvice ca = new CreditAdvice();
                ca.setMerchantId(merchant.getMerchantId());
                ca.setAccountNo("****" + String.format("%04d", random.nextInt(10000)));
                ca.setAccountId("ACC-" + merchant.getMerchantId() + "-" + (i + 1));
                ca.setCurrency("MYR");

                BigDecimal amount = BigDecimal.valueOf(1000 + random.nextDouble() * 9000).setScale(2, RoundingMode.HALF_UP);
                ca.setAmount(amount);

                LocalDateTime paymentDate = LocalDateTime.now().minusWeeks(i + 1);
                ca.setPaymentDate(paymentDate);

                allAdvices.add(creditAdviceRepository.save(ca));
            }
        }

        log.info("Created {} credit advices", allAdvices.size());
        return allAdvices;
    }

    private void seedSettlements(List<CreditAdvice> creditAdvices) {
        log.info("Seeding settlements...");
        int settlementCount = 0;
        String[] types = {"NORMAL", "NORMAL", "ADJUSTMENT", "CHARGEBACK"};

        for (CreditAdvice ca : creditAdvices) {
            int numSettlements = 1 + random.nextInt(2);
            for (int i = 0; i < numSettlements; i++) {
                Settlement s = new Settlement();
                s.setCreditAdviceId(ca.getCreditAdviceId());
                s.setSettlementNo("STL" + String.format("%08d", random.nextInt(100000000)));
                s.setSettlementType(types[random.nextInt(types.length)]);
                s.setCurrency("MYR");

                BigDecimal settlementAmount = ca.getAmount()
                        .divide(BigDecimal.valueOf(numSettlements), 2, RoundingMode.HALF_UP);
                s.setSettlementAmount(settlementAmount);

                BigDecimal paymentAmount = settlementAmount.multiply(BigDecimal.valueOf(0.975)).setScale(2, RoundingMode.HALF_UP);
                s.setPaymentAmount(paymentAmount);

                s.setSettlementDate(ca.getPaymentDate().plusDays(1 + random.nextInt(3)));

                settlementRepository.save(s);
                settlementCount++;
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
