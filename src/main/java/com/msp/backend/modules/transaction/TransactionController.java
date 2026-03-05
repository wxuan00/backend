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
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantRepository merchantRepository;

    @GetMapping
    public List<Transaction> getAllTransactions() {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return transactionService.getAllTransactions();
        } else {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null) return List.of();
            return transactionService.getTransactionsByMerchantId(merchantId);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable Long id) {
        Transaction txn = transactionService.getTransactionById(id);
        User currentUser = getCurrentUser();

        if (!"ADMIN".equals(currentUser.getRole())) {
            Long merchantId = getMyMerchantId(currentUser);
            if (merchantId == null || !merchantId.equals(txn.getMerchantId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(txn);
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
