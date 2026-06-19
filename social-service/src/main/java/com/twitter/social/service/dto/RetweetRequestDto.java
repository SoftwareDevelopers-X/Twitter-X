package com.twitter.social.service.dto;

import lombok.Data;

@Data
public class RetweetRequestDto {

    private Long userId;
    private Long tweetId;
}