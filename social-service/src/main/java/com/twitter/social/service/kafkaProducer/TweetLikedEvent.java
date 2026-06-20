package com.twitter.social.service.kafkaProducer;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TweetLikedEvent {

    private Long tweetId;

    private Long userId;
}