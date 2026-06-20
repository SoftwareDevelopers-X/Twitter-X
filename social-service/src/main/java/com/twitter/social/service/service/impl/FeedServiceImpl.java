package com.twitter.social.service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.social.service.cache.RedisService;
import com.twitter.social.service.client.TweetClient;
import com.twitter.social.service.dto.FeedTweetDto;
import com.twitter.social.service.service.FeedService;
import com.twitter.social.service.service.FollowService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FollowService followService;
    private final TweetClient tweetClient;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @CircuitBreaker(name = "tweetService", fallbackMethod = "fallbackFeed")
    @Override
    public List<FeedTweetDto> getFeed(Long userId, Long page, Long size) {

        try {

            String cacheKey = "feed::" + userId;
            if (redisService.exists(cacheKey)) {
                String cachedJson = (String) redisService.get(cacheKey);
                List<FeedTweetDto> cached =
                        objectMapper.readValue(
                                cachedJson,
                                objectMapper.getTypeFactory()
                                        .constructCollectionType(List.class, FeedTweetDto.class)
                        );

                return paginate(cached, page, size);
            }

            List<Long> followingUsers = followService.getFollowing(userId);

            List<FeedTweetDto> feedTweets =
                    tweetClient.getTweetsByUserIds(followingUsers);

            for (FeedTweetDto tweet : feedTweets) {
                tweet.setScore(calculateScore(tweet));
            }

            feedTweets.sort((a, b) -> {
                int scoreCompare = b.getScore().compareTo(a.getScore());
                if (scoreCompare == 0) {
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                }
                return scoreCompare;
            });

            String json = objectMapper.writeValueAsString(feedTweets);
            redisService.set(cacheKey, json, 10);
            return paginate(feedTweets, page, size);
        } catch (Exception e) {
            throw new RuntimeException("Error generating feed", e);
        }
    }

    public List<FeedTweetDto> fallbackFeed(Long userId, Long page, Long size, Exception ex) {

        String cacheKey = "feed::" + userId;

        if (redisService.exists(cacheKey)) {

            String cachedJson = (String) redisService.get(cacheKey);

            try {
                List<FeedTweetDto> cached =
                        objectMapper.readValue(
                                cachedJson,
                                objectMapper.getTypeFactory()
                                        .constructCollectionType(List.class, FeedTweetDto.class)
                        );

                return paginate(cached, page, size);

            } catch (Exception e) {
                return new ArrayList<>();
            }
        }

        return new ArrayList<>();
    }

    private double calculateScore(FeedTweetDto tweet) {

        double score = 0;

        score += tweet.getLikeCount() * 1.0;
        score += tweet.getRetweetCount() * 2.0;
        score += tweet.getReplyCount() * 1.5;

        return score;
    }

    private List<FeedTweetDto> paginate(List<FeedTweetDto> list, Long page, Long size) {

        Long start = page * size;
        Long end = Math.min(start + size, list.size());

        if (start >= list.size()) {
            return new ArrayList<>();
        }

        return list.subList(Math.toIntExact(start), Math.toIntExact(end));
    }
}