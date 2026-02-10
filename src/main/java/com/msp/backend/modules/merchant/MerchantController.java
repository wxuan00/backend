package com.msp.backend.modules.merchant;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:4200") // Allow Angular
public class MerchantController {

    private final MerchantService merchantService;
    private final UserRepository userRepository;

    @GetMapping
    public List<Merchant> getAllMerchants() {
        // 1. Get the currently logged-in user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User currentUser = userRepository.findByEmail(email).orElseThrow();

        // 2. CHECK ROLE
        if ("ADMIN".equals(currentUser.getRole())) {
            // Admin sees EVERYTHING
            return merchantService.getAllMerchants();
        } else {
            // Merchant sees ONLY THEIR OWN business
            // We return a list containing only their specific merchant details
            if (currentUser.getMerchantId() == null) {
                return List.of(); // Error safety
            }
            return merchantService.getAllMerchants().stream()
                    .filter(m -> m.getId().equals(currentUser.getMerchantId()))
                    .toList();
        }
    }

    @PostMapping
    public ResponseEntity<Merchant> createMerchant(@RequestBody Merchant merchant) {
        return ResponseEntity.ok(merchantService.createMerchant(merchant));
    }

    @GetMapping("/search")
    public List<Merchant> searchMerchants(@RequestParam String name) {
        return merchantService.searchMerchants(name);
    }
}