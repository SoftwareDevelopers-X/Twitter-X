package com.twitter.tweet.service.events.producer;

import com.twitter.events.TweetCreatedEvent;
import com.twitter.events.TweetDeletedEvent;
import com.twitter.events.TweetUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TweetProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    public void publishTweetCreatedEvent(TweetCreatedEvent event) {
        kafkaTemplate.send("tweet-created-topic", event);
    }

    public void publishTweetUpdatedEvent(TweetUpdatedEvent event) {
        kafkaTemplate.send("tweet-updated-topic", event);
    }

    public void publishTweetDeletedEvent(TweetDeletedEvent event) {
        kafkaTemplate.send("tweet-deleted-topic", event);
    }
}
