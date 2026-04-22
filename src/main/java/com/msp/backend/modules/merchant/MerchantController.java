package com.msp.backend.modules.merchant;

import com.msp.backend.modules.user.User;
import com.msp.backend.modules.user.UserRepository;
import com.msp.backend.modules.user.UserService;
import com.msp.backend.util.AuditHelper;
import com.msp.backend.util.MerchantResolver;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;
    private final MerchantRepository merchantRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final MerchantUserMappingRepository merchantUserMappingRepository;
    private final MerchantResolver merchantResolver;

    @GetMapping
    public List<Merchant> getAllMerchants() {
        User currentUser = getCurrentUser();
        if ("ADMIN".equals(currentUser.getRole())) {
            return merchantService.getAllMerchants();
        } else {
            Long myMerchantId = merchantResolver.resolveForUser(currentUser);
            if (myMerchantId == null) return List.of();
            return merchantRepository.findById(myMerchantId)
                    .map(List::of).orElse(List.of());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Merchant> getMerchantById(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        Merchant merchant = merchantService.getMerchantById(id);

        if (!"ADMIN".equals(currentUser.getRole())) {
            Long myMerchantId = merchantResolver.resolveForUser(currentUser);
            if (myMerchantId == null || !myMerchantId.equals(merchant.getMerchantId())) {
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
            Long myMerchantId = merchantResolver.resolveForUser(currentUser);
            if (myMerchantId == null) return List.of();
            final Long mid = myMerchantId;
            return merchantService.searchMerchants(name).stream()
                    .filter(m -> m.getMerchantId().equals(mid))
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

    // ── Merchant ↔ User Mappings (admin only) ───────────────────────────────

    @GetMapping("/{id}/users")
    public ResponseEntity<?> getMerchantUsers(@PathVariable Long id) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) return ResponseEntity.status(403).build();

        List<MerchantUserMapping> mappings = merchantUserMappingRepository.findByMerchantId(id);
        List<Map<String, Object>> result = mappings.stream().map(m -> {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("mappingId", m.getId());
            row.put("userId", m.getUserId());
            row.put("createdDate", m.getCreatedDate());
            row.put("createdBy", m.getCreatedBy());
            row.put("lastModifiedDate", m.getLastModifiedDate());
            row.put("lastModifiedBy", m.getLastModifiedBy());
            userRepository.findById(m.getUserId()).ifPresent(u -> {
                row.put("email", u.getEmail());
                row.put("firstName", u.getFirstName());
                row.put("lastName", u.getLastName());
                row.put("displayName", u.getDisplayName());
                row.put("status", u.getStatus());
            });
            return row;
        }).toList();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/{id}/users/{userId}")
    @Transactional
    public ResponseEntity<?> assignUserToMerchant(@PathVariable Long id, @PathVariable Long userId) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) return ResponseEntity.status(403).build();

        if (!merchantRepository.existsById(id))
            return ResponseEntity.notFound().build();
        if (!userRepository.existsById(userId))
            return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
        if (merchantUserMappingRepository.existsByMerchantIdAndUserId(id, userId))
            return ResponseEntity.badRequest().body(Map.of("error", "User already mapped to this merchant"));

        MerchantUserMapping mapping = new MerchantUserMapping();
        mapping.setMerchantId(id);
        mapping.setUserId(userId);
        String actor = AuditHelper.currentUser();
        mapping.setCreatedBy(actor);
        mapping.setLastModifiedBy(actor);
        merchantUserMappingRepository.save(mapping);
        return ResponseEntity.ok(Map.of("message", "User assigned successfully"));
    }

    @DeleteMapping("/{id}/users/{userId}")
    @Transactional
    public ResponseEntity<?> removeUserFromMerchant(@PathVariable Long id, @PathVariable Long userId) {
        User currentUser = getCurrentUser();
        if (!"ADMIN".equals(currentUser.getRole())) return ResponseEntity.status(403).build();

        if (!merchantUserMappingRepository.existsByMerchantIdAndUserId(id, userId))
            return ResponseEntity.notFound().build();
        merchantUserMappingRepository.deleteByMerchantIdAndUserId(id, userId);
        return ResponseEntity.ok(Map.of("message", "User removed successfully"));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        User user = userRepository.findByEmailAndDeletedAtIsNull(email).orElseThrow();
        userService.populateRole(user);
        return user;
    }
}
