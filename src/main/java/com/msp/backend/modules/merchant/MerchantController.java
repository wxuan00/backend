package com.msp.backend.modules.merchant;

import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @GetMapping
    public List<Merchant> getAllMerchants() {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return merchantService.getAllMerchants();
        } else {
            Merchant myMerchant = merchantRepository.findByUserId(currentUser.getUserId()).orElse(null);
            if (myMerchant == null) return List.of();
            return List.of(myMerchant);
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Merchant> getMerchantById(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Merchant merchant = merchantService.getMerchantById(id);

        if (!"ADMIN".equals(currentUser.getRole())) {
            Merchant myMerchant = merchantRepository.findByUserId(currentUser.getUserId()).orElse(null);
            if (myMerchant == null || !myMerchant.getMerchantId().equals(merchant.getMerchantId())) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(merchant);
    }

    @PostMapping
    public ResponseEntity<Merchant> createMerchant(@Valid @RequestBody Merchant merchant) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }
        Merchant created = merchantService.createMerchant(merchant);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/search")
    public List<Merchant> searchMerchants(@RequestParam String name) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) {
            Merchant myMerchant = merchantRepository.findByUserId(currentUser.getUserId()).orElse(null);
            if (myMerchant == null) return List.of();
            return merchantService.searchMerchants(name).stream()
                    .filter(m -> m.getMerchantId().equals(myMerchant.getMerchantId()))
                    .toList();
        }
        return merchantService.searchMerchants(name);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Merchant> updateMerchant(@PathVariable Long id, @Valid @RequestBody Merchant merchant) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }
        Merchant updated = merchantService.updateMerchant(id, merchant);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMerchant(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) {
            return ResponseEntity.status(403).build();
        }
        merchantService.deleteMerchant(id);
        return ResponseEntity.noContent().build();
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmail(email).orElseThrow();
        userService.populateRole(user);
        return user;
    }
}
