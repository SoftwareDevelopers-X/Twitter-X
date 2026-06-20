package com.twitter.tweet.service.service.Impl;

import com.twitter.tweet.service.constants.TrendingConstants;
import com.twitter.tweet.service.service.TrendingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrendingServiceImpl implements TrendingService {

    private final RedisTemplate<String,String> redisTemplate;

    @Override
    public void increaseLikeScore(Long tweetId) {
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_6H, tweetId.toString(), 1);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_24H, tweetId.toString(), 1);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_48H, tweetId.toString(), 1);
    }

    @Override
    public void increaseReplyScore(Long tweetId) {
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_6H, tweetId.toString(), 2);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_24H, tweetId.toString(), 2);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_48H, tweetId.toString(), 2);
    }

    @Override
    public void increaseRetweetScore(Long tweetId) {
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_6H, tweetId.toString(), 3);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_24H, tweetId.toString(), 3);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_48H, tweetId.toString(), 3);
    }

    @Override
    public void decreaseLikeScore(Long tweetId) {
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_6H, tweetId.toString(), -1);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_24H, tweetId.toString(), -1);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_48H, tweetId.toString(), -1);
    }

    @Override
    public void decreaseReplyScore(Long tweetId) {
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_6H, tweetId.toString(), -2);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_24H, tweetId.toString(), -2);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_48H, tweetId.toString(), -2);
    }

    @Override
    public void decreaseRetweetScore(Long tweetId) {
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_6H, tweetId.toString(), -3);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_24H, tweetId.toString(), -3);
        redisTemplate.opsForZSet().incrementScore(TrendingConstants.TRENDING_48H, tweetId.toString(), -3);
    }

    @Override
    public Set<String> getTrendingTweets(String window) {
        String key = switch (window) {
            case "6h" -> TrendingConstants.TRENDING_6H;
            case "24h" -> TrendingConstants.TRENDING_24H;
            default -> TrendingConstants.TRENDING_48H;
        };
        return redisTemplate.opsForZSet().reverseRange(key, 0, 19);
    }
}