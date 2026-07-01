package com.twitter.tweet.service.service.Impl;

import com.twitter.tweet.service.exceptions.customExceptions.InvalidTrendingWindowException;
import com.twitter.tweet.service.model.Tweet;
import com.twitter.tweet.service.repository.TweetRepository;
import com.twitter.tweet.service.service.TrendingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

@Service
@RequiredArgsConstructor
public class TrendingServiceImpl implements TrendingService {

    private final TweetRepository tweetRepository;

    @Override
    public List<Tweet> getTrendingTweets(String window) {
        LocalDateTime fromTime = switch (window) {
            case "6h" -> LocalDateTime.now().minusHours(6);
            case "24h" -> LocalDateTime.now().minusHours(24);
            case "48h" -> LocalDateTime.now().minusHours(48);
            default -> throw new InvalidTrendingWindowException(
                    "Window must be 6h, 24h or 48h");
        };
        List<Tweet> tweets = tweetRepository.findByCreatedAtAfter(fromTime);
        PriorityQueue<Tweet> minHeap = new PriorityQueue<>(
                Comparator.comparingDouble(this::calculateTrendingScore)
        );

        for (Tweet tweet : tweets) {
            minHeap.offer(tweet);
            if (minHeap.size() > 20) {
                minHeap.poll();
            }
        }
        List<Tweet> trendingTweets = new ArrayList<>(minHeap);
        trendingTweets.sort((t1, t2) ->
                Double.compare(
                        calculateTrendingScore(t2),
                        calculateTrendingScore(t1)
                ));
        return trendingTweets;
    }

    private double calculateTrendingScore(Tweet tweet) {
        double engagement = tweet.getLikeCount() + tweet.getReplyCount() * 2 + tweet.getRetweetCount() * 3;
        double ageHours = Duration.between(tweet.getCreatedAt(), LocalDateTime.now()).toHours();
        return engagement / Math.pow(ageHours + 2, 1.5);
    }
}