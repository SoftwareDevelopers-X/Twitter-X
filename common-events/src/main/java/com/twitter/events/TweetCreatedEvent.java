package com.twitter.events;

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
public class TweetCreatedEvent {

    private Long tweetId;

    private Long userId;

    private String content;

    private List<String> hashtags;
    private List<String> mediaUrls;

    private Long likeCount;

    private Long replyCount;

    private Long retweetCount;

    private Long viewCount;

    private LocalDateTime createdAt;

    private String username;
}
