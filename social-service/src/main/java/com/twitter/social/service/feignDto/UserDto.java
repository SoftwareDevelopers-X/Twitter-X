package com.twitter.social.service.feignDto;

import lombok.*;

/**
 * !!! VERIFY AGAINST YOUR ACTUAL auth-service RESPONSE DTO !!!
 *
 * Minimal fields social-service needs from auth-service to enrich a
 * ProfileResponse (username/displayName shown next to bio/avatar).
 * Adjust field names to match auth-service's real UserResponse.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Long userId;
    private String username;
    private String displayName;
    private String email;
}
