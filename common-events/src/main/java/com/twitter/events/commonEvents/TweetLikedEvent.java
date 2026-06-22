package com.twitter.events.commonEvents;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TweetLikedEvent {

    private Long tweetId;

    private Long userId;
}