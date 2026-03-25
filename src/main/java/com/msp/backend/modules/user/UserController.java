package com.msp.backend.modules.user;

import com.msp.backend.modules.role.Permission;
import com.msp.backend.modules.role.PermissionRepository;
import com.msp.backend.modules.role.RolePermissionRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
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

    // GET http://localhost:8080/api/users
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
        Long roleId = userRoles.isEmpty() ? null : userRoles.get(0).getRoleId();

        // Get permissions via role
        List<Permission> permissions = List.of();
        if (roleId != null) {
            List<Long> permIds = rolePermissionRepository.findByRoleId(roleId)
                .stream().map(rp -> rp.getPermissionId()).collect(Collectors.toList());
            if (!permIds.isEmpty()) {
                permissions = permissionRepository.findAllById(permIds);
            }
        }

        Map<String, Object> result = Map.of(
            "userId", user.getUserId(),
            "firstName", user.getFirstName() != null ? user.getFirstName() : "",
            "lastName", user.getLastName() != null ? user.getLastName() : "",
            "email", user.getEmail(),
            "contactNumber", user.getContactNumber() != null ? user.getContactNumber() : "",
            "displayName", user.getDisplayName() != null ? user.getDisplayName() : "",
            "status", user.getStatus() != null ? user.getStatus() : "",
            "role", user.getRole() != null ? user.getRole() : "",
            "mfaEnabled", user.isMfaEnabled(),
            "permissions", permissions
        );
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
    public ResponseEntity<User> updateUser(@PathVariable Long id, @Valid @RequestBody User user) {
        User updated = userService.updateUser(id, user);
        return ResponseEntity.ok(updated);
    }

    // 2. DELETE ENDPOINT
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}