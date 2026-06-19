package com.twitter.social.service.dto;

import lombok.Data;

@Data
public class LikeRequestDto {

    private Long userId;
    private Long tweetId;
    
}