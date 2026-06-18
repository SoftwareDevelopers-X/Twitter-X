package com.twitter.social.service.dto;

import lombok.Data;

@Data
public class FollowRequestDto {

    private Long followerId;

    private Long followingId;
}