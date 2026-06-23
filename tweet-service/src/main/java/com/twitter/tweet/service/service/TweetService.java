package com.twitter.tweet.service.service;

import com.twitter.tweet.service.dto.request.TweetRequest;
import com.twitter.tweet.service.dto.request.UpdateTweetRequest;
import com.twitter.tweet.service.dto.response.TweetResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface TweetService {
    TweetResponse createTweet(TweetRequest request, Long userId);

    TweetResponse getTweet(Long tweetId);

    TweetResponse updateTweet(Long tweetId, UpdateTweetRequest request, Long userId);

    void deleteTweet(Long tweetId, Long userId, String role);

    List<TweetResponse> getUserTweets(Long userId);

    List<TweetResponse> getTweetsByHashtag(String hashtag);

    Page<TweetResponse> getAllTweets(int page, int size);

    List<TweetResponse> getTrendingTweets(String window);

    List<TweetResponse> searchTweets(String keyword);

    List<TweetResponse> searchSuggestions(String keyword);

    List<TweetResponse> getTweetsByUserIds(List<Long> userIds);

    List<com.twitter.tweet.service.dto.response.HashtagResponse> getTrendingHashtags();
}
