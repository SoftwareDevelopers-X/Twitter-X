package com.twitter.auth.service.dto;

import com.twitter.auth.service.Enum.Role;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private Long userId;
    private String username;
    private String email;
    private Role role;
    private boolean enabled;
    private boolean accountLocked;
}