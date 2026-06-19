package com.twitter.tweet.service.scheduler;

import com.twitter.tweet.service.constants.TrendingConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TrendingCleanupScheduler {
    private final RedisTemplate<String,String> redisTemplate;

    @Scheduled(fixedRate = 21600000)
    public void clear6HourTrending() {
        redisTemplate.delete(TrendingConstants.TRENDING_6H);
    }

    @Scheduled(fixedRate = 86400000)
    public void clear24HourTrending() {
        redisTemplate.delete(TrendingConstants.TRENDING_24H);
    }

    @Scheduled(fixedRate = 172800000)
    public void clear48HourTrending() {
        redisTemplate.delete(TrendingConstants.TRENDING_48H);
    }
}
