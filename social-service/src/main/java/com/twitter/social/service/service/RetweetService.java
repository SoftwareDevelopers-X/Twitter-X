package com.twitter.social.service.service;

import com.twitter.social.service.dto.RetweetRequestDto;

import java.util.List;

public interface RetweetService {

    String retweet(RetweetRequestDto request);

    String undoRetweet(RetweetRequestDto request);

    boolean isRetweeted(Long userId, Long tweetId);

    Long getRetweetCount(Long tweetId);
}