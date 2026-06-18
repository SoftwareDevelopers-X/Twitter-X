package com.twitter.social.service.dto;

import lombok.Data;

@Data
public class BookmarkRequestDto {

    private Long userId;
    private Long tweetId;
}