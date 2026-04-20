package com.msp.backend.modules.user;

import java.io.Serializable;
import lombok.Data;

@Data
public class UserRoleId implements Serializable {
    private String userId;
    private Long roleId;
}
