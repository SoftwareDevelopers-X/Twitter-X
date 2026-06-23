package com.twitter.tweet.service.scheduler;

import com.twitter.tweet.service.model.Tweet;
import com.twitter.tweet.service.repository.TweetRepository;
import com.twitter.tweet.service.repository.elastic.TweetSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountScheduler {

    private final RedisTemplate<String, String> redisTemplate;
    private final TweetRepository tweetRepository;
    private final TweetSearchRepository tweetSearchRepository;

    @Scheduled(fixedRate = 300000)
    @Transactional
    public void flushViewCounts() {
        Set<String> keys = redisTemplate.keys("tweet:view:*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        for (String key : keys) {
            Long tweetId =
                    Long.parseLong(
                            key.replace("tweet:view:", "")
                    );

            String views = redisTemplate.opsForValue().get(key);
            if (views != null) {
                Tweet tweet = tweetRepository.findById(tweetId).orElse(null);
                if (tweet != null) {
                    tweet.setViewCount(tweet.getViewCount() + Integer.parseInt(views));
                    tweetRepository.save(tweet);
                    tweetSearchRepository.findById(tweetId).ifPresent(doc -> {
                        doc.setViewCount(tweet.getViewCount());
                        tweetSearchRepository.save(doc);
                    });
                }
                redisTemplate.delete(key);
            }
        }
        log.info("View counts flushed successfully");
    }
}