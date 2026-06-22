package com.twitter.social.service.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ReplyDto {
    private Long replyId;
    private Long userId;
    private Long tweetId;
    private String content;
    private LocalDateTime repliedAt;
}