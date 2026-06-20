package com.twitter.social.service.kafkaProducer;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TweetRepliedEvent {

    private Long tweetId;

    private Long userId;
}
