package com.twitter.notification.service.consumer;

import com.twitter.events.commonEvents.*;
import com.twitter.notification.service.dto.NotificationResponse;
import com.twitter.notification.service.service.NotificationService;
import com.twitter.notification.service.websocket.NotificationWebSocketHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final NotificationWebSocketHandler webSocketHandler;

    @KafkaListener(groupId = "notification-group", topics = "notification-topic")
    public void consume(NotificationEventDto event) {
        log.info("Received notification event {}", event);
        if (event.getSenderUserId() != null && event.getSenderUserId().equals(event.getReceiverUserId())) {
            log.info("Skipping self-notification");
            return;
        }
        NotificationResponse savedNotification = notificationService.createNotification(event);
        if (savedNotification != null) {
            webSocketHandler.sendNotification(event.getReceiverUserId(), savedNotification);
        }
    }

    @KafkaListener(topics = "tweet-liked-topic", groupId = "notification-interaction-group")
    public void consumeTweetLikedEvent(TweetLikedEvent event) {
        log.info("Received TweetLikedEvent: {}", event);
        webSocketHandler.broadcast("TWEET_LIKED", Map.of(
                "tweetId", event.getTweetId(),
                "userId", event.getUserId()
        ));
    }

    @KafkaListener(topics = "tweet-unliked-topic", groupId = "notification-interaction-group")
    public void consumeTweetUnlikedEvent(TweetUnlikedEvent event) {
        log.info("Received TweetUnlikedEvent: {}", event);
        webSocketHandler.broadcast("TWEET_UNLIKED", Map.of(
                "tweetId", event.getTweetId(),
                "userId", event.getUserId()
        ));
    }

    @KafkaListener(topics = "tweet-retweeted-topic", groupId = "notification-interaction-group")
    public void consumeTweetRetweetedEvent(TweetRetweetedEvent event) {
        log.info("Received TweetRetweetedEvent: {}", event);
        webSocketHandler.broadcast("TWEET_RETWEETED", Map.of(
                "tweetId", event.getTweetId(),
                "userId", event.getUserId()
        ));
    }

    @KafkaListener(topics = "tweet-retweet-removed-topic", groupId = "notification-interaction-group")
    public void consumeTweetRetweetRemovedEvent(TweetRetweetRemovedEvent event) {
        log.info("Received TweetRetweetRemovedEvent: {}", event);
        webSocketHandler.broadcast("TWEET_RETWEET_REMOVED", Map.of(
                "tweetId", event.getTweetId(),
                "userId", event.getUserId()
        ));
    }

    @KafkaListener(topics = "tweet-replied-topic", groupId = "notification-interaction-group")
    public void consumeTweetRepliedEvent(TweetRepliedEvent event) {
        log.info("Received TweetRepliedEvent: {}", event);
        webSocketHandler.broadcast("TWEET_REPLIED", Map.of(
                "tweetId", event.getTweetId(),
                "userId", event.getUserId()
        ));
    }

    @KafkaListener(topics = "tweet-reply-deleted-topic", groupId = "notification-interaction-group")
    public void consumeTweetReplyDeletedEvent(TweetReplyDeletedEvent event) {
        log.info("Received TweetReplyDeletedEvent: {}", event);
        webSocketHandler.broadcast("TWEET_REPLY_DELETED", Map.of(
                "tweetId", event.getTweetId(),
                "userId", event.getUserId()
        ));
    }
}
