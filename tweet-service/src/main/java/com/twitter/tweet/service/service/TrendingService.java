package com.twitter.tweet.service.service;

import com.twitter.tweet.service.model.Tweet;

import java.util.List;
import java.util.Set;

public interface TrendingService {
    void increaseLikeScore(Long tweetId);
    void increaseReplyScore(Long tweetId);
    void increaseRetweetScore(Long tweetId);

    void decreaseLikeScore(Long tweetId);

    void decreaseReplyScore(Long tweetId);

    void decreaseRetweetScore(Long tweetId);
    List<Tweet> getTrendingTweets(String window);
}
