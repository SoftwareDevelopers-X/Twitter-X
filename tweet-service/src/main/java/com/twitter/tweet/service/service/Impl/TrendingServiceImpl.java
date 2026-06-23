package com.twitter.tweet.service.service.Impl;

import com.twitter.tweet.service.constants.TrendingConstants;
import com.twitter.tweet.service.exceptions.customExceptions.InvalidTrendingWindowException;
import com.twitter.tweet.service.model.Tweet;
import com.twitter.tweet.service.repository.TweetRepository;
import com.twitter.tweet.service.service.TrendingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TrendingServiceImpl implements TrendingService {

    private final RedisTemplate<String,String> redisTemplate;
    private final TweetRepository tweetRepository;

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
    public List<Tweet> getTrendingTweets(String window) {
        String key = switch (window) {
            case "6h" -> TrendingConstants.TRENDING_6H;
            case "24h" -> TrendingConstants.TRENDING_24H;
            case "48h" -> TrendingConstants.TRENDING_48H;
            default -> throw new InvalidTrendingWindowException("Window must be 6h, 24h or 48h");
        };

        Set<String> ids = redisTemplate.opsForZSet().reverseRange(key, 0, 99);
        List<Tweet> tweets;
        if (ids == null || ids.isEmpty()) {
            tweets = tweetRepository.findAll();
        } else {
            List<Long> tweetIds = ids.stream()
                            .map(Long::valueOf)
                            .toList();
            tweets = tweetRepository.findAllById(tweetIds);
        }

        java.util.ArrayList<Tweet> modifiableTweets = new java.util.ArrayList<>(tweets);

        modifiableTweets.sort((a, b) -> Double.compare(
                        calculateTrendingScore(b),
                        calculateTrendingScore(a)));

        return modifiableTweets.stream()
                .limit(20)
                .toList();
    }

    private double calculateTrendingScore(Tweet tweet) {

        double engagement =
                tweet.getLikeCount()
                        + tweet.getReplyCount() * 2
                        + tweet.getRetweetCount() * 3;

        double ageHours =
                Duration.between(
                                tweet.getCreatedAt(),
                                LocalDateTime.now())
                        .toHours();

        return engagement / Math.pow(ageHours + 2, 1.5);
    }
}