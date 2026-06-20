package com.twitter.auth.service.dto.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserManagementResponse {

    private Long userId;
    private String username;
    private String email;
    private String role;
    private boolean enabled;
    private boolean accountLocked;
}