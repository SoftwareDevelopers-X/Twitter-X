package com.twitter.social.service.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twitter.social.service.cache.RedisService;
import com.twitter.social.service.client.TweetServiceClient;
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
    private final TweetServiceClient tweetServiceClient;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    @Override
    public List<FeedTweetDto> getFeed(Long userId, int page, int size) {

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
                    tweetServiceClient.getTweetsByUserIds(followingUsers);

            for (FeedTweetDto tweet : feedTweets) {
                tweet.setScore(calculateScore(tweet));
            }

            feedTweets.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));

            String json = objectMapper.writeValueAsString(feedTweets);
            redisService.set(cacheKey, json, 10);
            return paginate(feedTweets, page, size);
        } catch (Exception e) {
            throw new RuntimeException("Error generating feed", e);
        }
    }


    private double calculateScore(FeedTweetDto tweet) {

        double score = 0;

        score += tweet.getLikeCount() * 1.0;
        score += tweet.getRetweetCount() * 2.0;
        score += tweet.getReplyCount() * 1.5;

        return score;
    }

    private List<FeedTweetDto> paginate(List<FeedTweetDto> list, int page, int size) {

        int start = page * size;
        int end = Math.min(start + size, list.size());

        if (start >= list.size()) {
            return new ArrayList<>();
        }

        return list.subList(start, end);
    }
}