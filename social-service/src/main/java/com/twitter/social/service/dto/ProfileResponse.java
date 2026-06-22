package com.twitter.social.service.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProfileResponse {

    private Long userId;

    // Enriched from auth-service via Feign
    private String username;
    private String displayName;

    private String bio;
    private String location;
    private String website;
    private String avatarUrl;
    private String bannerUrl;
    private LocalDate dateOfBirth;
    private Boolean isVerified;
    private Boolean isPrivate;
    private LocalDateTime joinedAt;

    private Long followersCount;
    private Long followingCount;
    private Long postsCount;

    // true if the currently-authenticated user (from header/param) follows this profile
    private Boolean isFollowedByCurrentUser;

    // true if this profile belongs to the currently-authenticated user
    private Boolean isOwnProfile;
}
