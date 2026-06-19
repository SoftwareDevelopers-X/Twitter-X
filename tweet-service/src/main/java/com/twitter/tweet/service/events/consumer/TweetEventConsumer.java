package com.twitter.tweet.service.events.consumer;

import com.twitter.events.TweetLikedEvent;
import com.twitter.events.TweetRepliedEvent;
import com.twitter.events.TweetRetweetedEvent;
import com.twitter.tweet.service.service.TrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TweetEventConsumer {

    private final TrendingService trendingService;

    @KafkaListener(topics = "tweet-liked-topic", groupId = "tweet-group")
    public void consumeLikeEvent(TweetLikedEvent event) {
        log.info("Like event received for tweet {}", event.getTweetId());
        trendingService.increaseLikeScore(event.getTweetId());
    }

    @KafkaListener(topics = "tweet-replied-topic", groupId = "tweet-group")
    public void consumeReplyEvent(TweetRepliedEvent event) {
        trendingService.increaseReplyScore(event.getTweetId());
    }

    @KafkaListener(topics = "tweet-retweeted-topic", groupId = "tweet-group")
    public void consumeRetweetEvent(TweetRetweetedEvent event) {
        trendingService.increaseRetweetScore(event.getTweetId());
    }

}
