package com.twitter.social.service.service;

import com.twitter.social.service.dto.LikeRequestDto;

public interface LikeService {

    String likeTweet(LikeRequestDto request);

    String unlikeTweet(LikeRequestDto request);

    Long getLikeCount(Long tweetId);

    boolean isTweetLiked(Long userId, Long tweetId);
}