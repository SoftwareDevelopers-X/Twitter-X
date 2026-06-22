package com.twitter.tweet.service.events.consumer;


import com.twitter.events.commonEvents.*;
import com.twitter.tweet.service.model.Tweet;
import com.twitter.tweet.service.repository.TweetRepository;
import com.twitter.tweet.service.service.TrendingService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TweetInteractionConsumer {

    private final TweetRepository tweetRepository;
    private final TrendingService trendingService;

    @PostConstruct
    public void init() {
        System.out.println("TweetInteractionConsumer initialized");
    }

    @KafkaListener(topics = "tweet-liked-topic", groupId = "tweet-group")
    public void consumeTweetLikedEvent(TweetLikedEvent event) {
        System.out.println("EVENT RECEIVED");
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setLikeCount(tweet.getLikeCount() + 1);
        tweetRepository.save(tweet);
        trendingService.increaseLikeScore(event.getTweetId());
    }

    @KafkaListener(topics = "tweet-replied-topic", groupId = "tweet-group")
    public void consumeTweetRepliedEvent(TweetRepliedEvent event) {
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setReplyCount(tweet.getReplyCount() + 1);
        tweetRepository.save(tweet);
        trendingService.increaseReplyScore(event.getTweetId());
    }

    @KafkaListener(topics = "tweet-retweeted-topic", groupId = "tweet-group")
    public void consumeTweetRetweetedEvent(TweetRetweetedEvent event) {
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setRetweetCount(tweet.getRetweetCount() + 1);
        tweetRepository.save(tweet);
        trendingService.increaseRetweetScore(event.getTweetId());
    }

    @KafkaListener(topics = "tweet-unliked-topic", groupId = "tweet-group")
    public void consumeUnlikedEvent(TweetUnlikedEvent event){
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setLikeCount(Math.max(0, tweet.getLikeCount() - 1));
        tweetRepository.save(tweet);
        trendingService.decreaseLikeScore(event.getTweetId());
    }

    @KafkaListener(topics = "tweet-reply-deleted-topic", groupId = "tweet-group")
    public void consumeReplyDeletedEvent(TweetReplyDeletedEvent event){
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setReplyCount(Math.max(0, tweet.getReplyCount() - 1));
        tweetRepository.save(tweet);
        trendingService.decreaseReplyScore(event.getTweetId());
    }

    @KafkaListener(topics = "tweet-retweet-removed-topic", groupId = "tweet-group")
    public void consumeRetweetRemovedEvent(TweetRetweetRemovedEvent event){
        Tweet tweet = tweetRepository.findById(event.getTweetId()).orElseThrow();
        tweet.setRetweetCount(Math.max(0, tweet.getRetweetCount() - 1));
        tweetRepository.save(tweet);
        trendingService.decreaseRetweetScore(event.getTweetId());
    }

}
