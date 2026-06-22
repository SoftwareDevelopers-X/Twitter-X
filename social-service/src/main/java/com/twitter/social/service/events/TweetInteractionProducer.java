package com.twitter.social.service.events;

import com.twitter.events.commonEvents.*;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TweetInteractionProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTweetLikedEvent(TweetLikedEvent event) {
        System.out.println("send Like Event: " + event);
        kafkaTemplate.send("tweet-liked-topic", event);
    }

    public void publishTweetUnlikedEvent(TweetUnlikedEvent event) {
        kafkaTemplate.send("tweet-unliked-topic", event);
    }

    public void publishTweetRepliedEvent(TweetRepliedEvent event) {
        kafkaTemplate.send("tweet-replied-topic", event);
    }

    public void publishTweetReplyDeletedEvent(TweetReplyDeletedEvent event) {
        kafkaTemplate.send("tweet-reply-deleted-topic", event);
    }

    public void publishTweetRetweetedEvent(TweetRetweetedEvent event) {
        kafkaTemplate.send("tweet-retweeted-topic", event);
    }

    public void publishTweetRetweetRemovedEvent(TweetRetweetRemovedEvent event) {
        kafkaTemplate.send("tweet-retweet-removed-topic", event);
    }
}