package com.twitter.tweet.service.service;

import com.twitter.tweet.service.model.Tweet;

import java.util.List;
import java.util.Set;

public interface TrendingService {
    List<Tweet> getTrendingTweets(String window);
}
