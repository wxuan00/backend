package com.msp.backend.modules.role;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public Role getRoleById(Long id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Role not found"));
    }

    public Role createRole(Role role) {
        if (roleRepository.findByRoleName(role.getRoleName()).isPresent()) {
            throw new RuntimeException("Role name already exists");
        }
        return roleRepository.save(role);
    }

    public Role updateRole(Long id, Role updated) {
        Role role = getRoleById(id);
        role.setRoleName(updated.getRoleName());
        role.setDescription(updated.getDescription());
        role.setRoleType(updated.getRoleType());
        return roleRepository.save(role);
    }

    public void deleteRole(Long id) {
        Role role = getRoleById(id);
        if ("ADMIN".equals(role.getRoleName()) || "MERCHANT".equals(role.getRoleName())) {
            throw new RuntimeException("Cannot delete system role: " + role.getRoleName());
        }
        roleRepository.deleteById(id);
    }
}
