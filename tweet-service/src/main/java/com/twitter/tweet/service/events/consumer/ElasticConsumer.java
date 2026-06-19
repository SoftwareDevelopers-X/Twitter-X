package com.twitter.tweet.service.events.consumer;

import com.twitter.events.TweetCreatedEvent;
import com.twitter.events.TweetDeletedEvent;
import com.twitter.events.TweetUpdatedEvent;
import com.twitter.tweet.service.repository.elastic.TweetSearchRepository;
import com.twitter.tweet.service.search.TweetDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticConsumer {
    private final TweetSearchRepository tweetSearchRepository;

    @KafkaListener(topics = "tweet-created-topic", groupId = "elastic-group")
    public void consumeTweetCreatedEvent(TweetCreatedEvent event) {

        log.info("Indexing tweet {} into Elasticsearch", event.getTweetId());
        TweetDocument document = TweetDocument.builder()
                        .tweetId(event.getTweetId())
                        .userId(event.getUserId())
                        .content(event.getContent())
                        .hashtags(event.getHashtags())
                         .mediaUrls(event.getMediaUrls())
                        .likeCount(event.getLikeCount())
                        .replyCount(event.getReplyCount())
                        .retweetCount(event.getRetweetCount())
                        .viewCount(event.getViewCount())
                        .createdAt(event.getCreatedAt())
                        .build();

        tweetSearchRepository.save(document);
    }

    @KafkaListener(topics = "tweet-updated-topic", groupId = "elastic-group")
    public void consumeTweetUpdatedEvent(TweetUpdatedEvent event) {
        TweetDocument document = tweetSearchRepository.findById(event.getTweetId()).orElseThrow();
        document.setContent(event.getContent());
        document.setHashtags(event.getHashtags());
        tweetSearchRepository.save(document);
    }

    @KafkaListener(topics = "tweet-deleted-topic", groupId = "elastic-group")
    public void consumeTweetDeletedEvent(TweetDeletedEvent event) {
        tweetSearchRepository.deleteById(event.getTweetId());
    }
}
