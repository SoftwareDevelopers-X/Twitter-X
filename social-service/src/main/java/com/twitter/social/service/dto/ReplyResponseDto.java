package com.twitter.social.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReplyResponseDto {
    private Long replyId;
    private Long userId;
    private Long tweetId;
    private String content;
    private LocalDateTime repliedAt;
    private Long parentReplyId;

    private Long likeCount;
    private Long retweetCount;
    private Long replyCount;
    private Long viewCount;
    private Long bookmarkCount;

    @JsonProperty("isLiked")
    private boolean isLiked;

    @JsonProperty("isRetweeted")
    private boolean isRetweeted;

    @JsonProperty("isBookmarked")
    private boolean isBookmarked;
}
