package com.twitter.tweet.service.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class TweetRequest {
    private String content;
    private List<MediaRequest> mediaUrls;
    private List<String> hashtags;
}
