package com.twitter.social.service.dto;

import lombok.Data;

@Data
public class ReplyRequestDto {

    private Long userId;
    private Long tweetId;
    private String content;
    private Long parentReplyId;
}