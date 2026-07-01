package com.twitter.tweet.service.events.consumer;


import com.twitter.events.commonEvents.*;
import com.twitter.tweet.service.model.Tweet;
import com.twitter.tweet.service.repository.TweetRepository;
import com.twitter.tweet.service.repository.elastic.TweetSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TweetInteractionConsumer {

    private final TweetRepository tweetRepository;
    private final TweetSearchRepository tweetSearchRepository;


    @KafkaListener(topics = "tweet-liked-topic", groupId = "tweet-group")
    public void consumeTweetLikedEvent(TweetLikedEvent event) {
        log.info("Tweet liked event received");
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setLikeCount(tweet.getLikeCount() + 1);
        tweetRepository.save(tweet);
        tweetSearchRepository.findById(event.getTweetId()).ifPresent(doc -> {
            doc.setLikeCount(tweet.getLikeCount());
            tweetSearchRepository.save(doc);
        });
    }

    @KafkaListener(topics = "tweet-replied-topic", groupId = "tweet-group")
    public void consumeTweetRepliedEvent(TweetRepliedEvent event) {
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setReplyCount(tweet.getReplyCount() + 1);
        tweetRepository.save(tweet);
        tweetSearchRepository.findById(event.getTweetId()).ifPresent(doc -> {
            doc.setReplyCount(tweet.getReplyCount());
            tweetSearchRepository.save(doc);
        });
    }

    @KafkaListener(topics = "tweet-retweeted-topic", groupId = "tweet-group")
    public void consumeTweetRetweetedEvent(TweetRetweetedEvent event) {
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setRetweetCount(tweet.getRetweetCount() + 1);
        tweetRepository.save(tweet);
        tweetSearchRepository.findById(event.getTweetId()).ifPresent(doc -> {
            doc.setRetweetCount(tweet.getRetweetCount());
            tweetSearchRepository.save(doc);
        });
    }

    @KafkaListener(topics = "tweet-unliked-topic", groupId = "tweet-group")
    public void consumeUnlikedEvent(TweetUnlikedEvent event){
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setLikeCount(Math.max(0, tweet.getLikeCount() - 1));
        tweetRepository.save(tweet);
        tweetSearchRepository.findById(event.getTweetId()).ifPresent(doc -> {
            doc.setLikeCount(tweet.getLikeCount());
            tweetSearchRepository.save(doc);
        });
    }

    @KafkaListener(topics = "tweet-reply-deleted-topic", groupId = "tweet-group")
    public void consumeReplyDeletedEvent(TweetReplyDeletedEvent event){
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setReplyCount(Math.max(0, tweet.getReplyCount() - 1));
        tweetRepository.save(tweet);
        tweetSearchRepository.findById(event.getTweetId()).ifPresent(doc -> {
            doc.setReplyCount(tweet.getReplyCount());
            tweetSearchRepository.save(doc);
        });
    }

    @KafkaListener(topics = "tweet-retweet-removed-topic", groupId = "tweet-group")
    public void consumeRetweetRemovedEvent(TweetRetweetRemovedEvent event){
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setRetweetCount(Math.max(0, tweet.getRetweetCount() - 1));
        tweetRepository.save(tweet);
        tweetSearchRepository.findById(event.getTweetId()).ifPresent(doc -> {
            doc.setRetweetCount(tweet.getRetweetCount());
            tweetSearchRepository.save(doc);
        });
    }

}
