package com.twitter.tweet.service.scheduler;

import com.twitter.tweet.service.model.Tweet;
import com.twitter.tweet.service.repository.TweetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final TweetRepository tweetRepository;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void flushViewCounts() {

        List<Tweet> tweets = tweetRepository.findAll();
        for (Tweet tweet : tweets) {
            String key = "tweet:view:" + tweet.getTweetId();
            String views = redisTemplate.opsForValue().get(key);
            if (views != null) {
                tweet.setViewCount(
                        tweet.getViewCount()
                                + Integer.parseInt(views));
                tweetRepository.save(tweet);
                redisTemplate.delete(key);
            }
        }
        log.info("View counts flushed successfully");
    }
}