package com.twitter.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TweetUpdatedEvent {

    private Long tweetId;
    private String content;
    private List<String> hashtags;
}