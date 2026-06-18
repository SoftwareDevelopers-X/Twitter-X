package com.twitter.social.service.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FeedTweetDto {

    private Long tweetId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;

    private Long likeCount;
    private Long retweetCount;
    private Long replyCount;

    private Double score;
}