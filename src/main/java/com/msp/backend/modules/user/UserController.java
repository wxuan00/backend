package com.msp.backend.modules.user;

import com.msp.backend.modules.role.Permission;
import com.msp.backend.modules.role.PermissionRepository;
import com.msp.backend.modules.role.RolePermissionRepository;
import com.msp.backend.modules.role.RoleRepository;
import com.msp.backend.util.AuditHelper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor

public class UserController {

    private final UserService userService;
    private final UserRoleRepository userRoleRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;

    // GET http://localhost:8001/api/users
    @GetMapping
    public List<User> getAllUsers() {
        return userService.getAllUsers();
    }

    // GET single user by ID
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        User user = userService.getUserById(id);
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    // GET user with role + permissions details
    @GetMapping("/{id}/details")
    public ResponseEntity<Map<String, Object>> getUserDetails(@PathVariable Long id) {
        User user = userService.getUserById(id);
        user.setPassword(null);

        // Get role info
        List<UserRole> userRoles = userRoleRepository.findByUserId(id);

        // Build roles list with full role objects
        List<Map<String, Object>> rolesList = new ArrayList<>();
        List<Permission> permissions = List.of();
        for (UserRole ur : userRoles) {
            roleRepository.findById(ur.getRoleId()).ifPresent(role -> {
                Map<String, Object> rm = new HashMap<>();
                rm.put("roleId", role.getRoleId());
                rm.put("roleName", role.getRoleName());
                rm.put("roleType", role.getRoleType() != null ? role.getRoleType() : "");
                rm.put("description", role.getDescription() != null ? role.getDescription() : "");
                rolesList.add(rm);
            });
        }

        // Get permissions via first role
        if (!userRoles.isEmpty()) {
            Long roleId = userRoles.get(0).getRoleId();
            List<Long> permIds = rolePermissionRepository.findByRoleId(roleId)
                .stream().map(rp -> rp.getPermissionId()).collect(Collectors.toList());
            if (!permIds.isEmpty()) {
                permissions = permissionRepository.findAllById(permIds);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("userId", user.getUserId());
        result.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
        result.put("lastName", user.getLastName() != null ? user.getLastName() : "");
        result.put("email", user.getEmail());
        result.put("contactNumber", user.getContactNumber() != null ? user.getContactNumber() : "");
        result.put("displayName", user.getDisplayName() != null ? user.getDisplayName() : "");
        result.put("status", user.getStatus() != null ? user.getStatus() : "");
        result.put("role", user.getRole() != null ? user.getRole() : "");
        result.put("mfaEnabled", user.isMfaEnabled());
        result.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        result.put("createdBy", AuditHelper.resolveDisplayName(user.getCreatedBy(), userRepository));
        result.put("lastModifiedAt", user.getLastModifiedAt() != null ? user.getLastModifiedAt().toString() : "");
        result.put("lastModifiedBy", AuditHelper.resolveDisplayName(user.getLastModifiedBy(), userRepository));
        result.put("roles", rolesList);
        result.put("permissions", permissions);
        return ResponseEntity.ok(result);
    }

    // 1. ADD ENDPOINT
    @PostMapping
    public ResponseEntity<User> createUser(@Valid @RequestBody User user) {
        User created = userService.createUser(user);
        return ResponseEntity.ok(created);
    }

    // UPDATE user
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody Map<String, String> body) {
        User user = new User();
        user.setFirstName(body.get("firstName"));
        user.setLastName(body.get("lastName"));
        user.setDisplayName(body.get("displayName"));
        user.setContactNumber(body.get("contactNumber"));
        user.setStatus(body.get("status"));
        // password is intentionally never set here
        User updated = userService.updateUser(id, user);
        return ResponseEntity.ok(updated);
    }

    // 2. DELETE ENDPOINT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    // Sync all assigned roles for a user (replaces existing set)
    @Transactional
    @PutMapping("/{id}/roles")
    public ResponseEntity<Void> syncRoles(@PathVariable Long id, @RequestBody java.util.List<Long> roleIds) {
        String actor = AuditHelper.currentUser();
        userRoleRepository.deleteByUserId(id);
        for (Long roleId : roleIds) {
            if (roleId != null) {
                UserRole ur = new UserRole();
                ur.setUserId(id);
                ur.setRoleId(roleId);
                ur.setGeneratedBy(actor);
                ur.setLastModifiedBy(actor);
                ur.setLastModifiedAt(java.time.LocalDateTime.now());
                userRoleRepository.save(ur);
            }
        }
        return ResponseEntity.ok().build();
    }

    // Unassign a single role from user
    @DeleteMapping("/{id}/roles/{roleId}")
    public ResponseEntity<Void> unassignRole(@PathVariable Long id, @PathVariable Long roleId) {
        userRoleRepository.findByUserId(id).stream()
            .filter(ur -> ur.getRoleId().equals(roleId))
            .forEach(ur -> userRoleRepository.delete(ur));
        return ResponseEntity.ok().build();
    }

    // Reset user password (admin action)
    @PatchMapping("/{id}/password")
    public ResponseEntity<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        userService.resetPassword(id, newPassword);
        return ResponseEntity.ok().build();
    }
}