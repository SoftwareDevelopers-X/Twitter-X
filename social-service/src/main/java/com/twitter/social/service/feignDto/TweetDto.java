package com.twitter.social.service.feignDto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @Builder @AllArgsConstructor @NoArgsConstructor
public class TweetDto {
    private Long tweetId;
    private Long userId;
    private String content;
    private Long likeCount;
    private Long replyCount;
    private Long retweetCount;
    private Long viewCount;
    private LocalDateTime createdAt;
    private List<TweetMediaDto> mediaList; // empty list if no media -> drives the Media tab filter
}