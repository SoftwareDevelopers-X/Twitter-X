package com.twitter.tweet.service.events.socialTweetEvents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TweetLikedEvent {

    private Long tweetId;

    private Long userId;
}