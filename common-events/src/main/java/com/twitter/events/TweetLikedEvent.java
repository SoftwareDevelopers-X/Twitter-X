package com.twitter.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TweetLikedEvent {
    private Long tweetId;
    private Long senderUserId;
    private Long receiverUserId;
}
