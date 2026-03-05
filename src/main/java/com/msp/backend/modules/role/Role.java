package com.msp.backend.modules.role;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", nullable = false, unique = true)
    private String roleName; // e.g., ADMIN, MERCHANT

    private String description;

    @Column(name = "role_type")
    private String roleType; // e.g., SYSTEM, CUSTOM
}
