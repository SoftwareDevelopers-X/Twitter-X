package com.twitter.social.service.service;

import com.twitter.social.service.dto.FeedTweetDto;

import java.util.List;

public interface FeedService {

    List<FeedTweetDto> getFeed(Long userId, int page, int size);
}