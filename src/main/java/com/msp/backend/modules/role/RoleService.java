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
        if (roleRepository.findByName(role.getName()).isPresent()) {
            throw new RuntimeException("Role name already exists");
        }
        return roleRepository.save(role);
    }

    public Role updateRole(Long id, Role updated) {
        Role role = getRoleById(id);
        role.setName(updated.getName());
        role.setDescription(updated.getDescription());
        return roleRepository.save(role);
    }

    public void deleteRole(Long id) {
        Role role = getRoleById(id);
        // Prevent deletion of core roles
        if ("ADMIN".equals(role.getName()) || "MERCHANT".equals(role.getName())) {
            throw new RuntimeException("Cannot delete system role: " + role.getName());
        }
        roleRepository.deleteById(id);
    }
}
