package com.twitter.tweet.service.service;

import java.util.Set;

public interface TrendingService {
    void increaseLikeScore(Long tweetId);
    void increaseReplyScore(Long tweetId);
    void increaseRetweetScore(Long tweetId);
    Set<String> getTrendingTweets(String window);
}
