package com.twitter.tweet.service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
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
