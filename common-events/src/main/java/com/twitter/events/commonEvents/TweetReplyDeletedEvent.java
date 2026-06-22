package com.twitter.events.commonEvents;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TweetReplyDeletedEvent {

    private Long tweetId;

    private Long userId;
}
