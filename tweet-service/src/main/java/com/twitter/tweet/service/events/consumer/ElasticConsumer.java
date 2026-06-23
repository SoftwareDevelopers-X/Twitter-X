package com.twitter.tweet.service.events.consumer;

import com.twitter.events.TweetCreatedEvent;
import com.twitter.events.TweetDeletedEvent;
import com.twitter.events.TweetUpdatedEvent;
import com.twitter.tweet.service.repository.elastic.TweetSearchRepository;
import com.twitter.tweet.service.repository.TweetRepository;
import com.twitter.tweet.service.client.AuthServiceClient;
import com.twitter.tweet.service.search.TweetDocument;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticConsumer {
    private final TweetSearchRepository tweetSearchRepository;
    private final TweetRepository tweetRepository;
    private final AuthServiceClient authServiceClient;

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
                        .username(event.getUsername())
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

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void init() {
        log.info("ElasticConsumer bean initialized. Syncing PostgreSQL tweets to Elasticsearch...");
        try {
            List<com.twitter.tweet.service.model.Tweet> tweets = tweetRepository.findAll();
            List<TweetDocument> docs = tweets.stream().map(t -> {
                String username = "user";
                try {
                    var user = authServiceClient.getUserById(t.getUserId());
                    if (user != null && user.getUsername() != null) {
                        username = user.getUsername();
                    }
                } catch (Exception ex) {
                    log.warn("Failed to fetch username for userId={} during startup ES sync: {}", t.getUserId(), ex.getMessage());
                }

                List<String> hashtags = t.getTweetHashtags() == null ? List.of() :
                    t.getTweetHashtags().stream()
                        .map(th -> th.getHashtag().getName())
                        .toList();

                List<String> media = t.getMediaList() == null ? List.of() :
                    t.getMediaList().stream()
                        .map(m -> m.getMediaUrl())
                        .toList();

                return TweetDocument.builder()
                        .tweetId(t.getTweetId())
                        .userId(t.getUserId())
                        .content(t.getContent())
                        .hashtags(hashtags)
                        .mediaUrls(media)
                        .likeCount(t.getLikeCount() != null ? t.getLikeCount() : 0L)
                        .replyCount(t.getReplyCount() != null ? t.getReplyCount() : 0L)
                        .retweetCount(t.getRetweetCount() != null ? t.getRetweetCount() : 0L)
                        .viewCount(t.getViewCount() != null ? t.getViewCount() : 0L)
                        .createdAt(t.getCreatedAt())
                        .username(username)
                        .build();
            }).toList();

            tweetSearchRepository.saveAll(docs);
            log.info("Successfully synced {} tweets to Elasticsearch index", docs.size());
        } catch (Exception e) {
            log.error("Failed to perform startup Elasticsearch sync", e);
        }
    }
}
