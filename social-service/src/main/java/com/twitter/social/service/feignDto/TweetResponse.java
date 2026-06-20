package com.twitter.social.service.feignDto;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class TweetResponse {

    private Long tweetId;
    private Long userId;
    private String content;
    private List<String> mediaUrls;
    private List<String> hashtags;
    private Long likeCount;
    private Long replyCount;
    private Long retweetCount;
    private Long viewCount;
    private LocalDateTime createdAt;
}