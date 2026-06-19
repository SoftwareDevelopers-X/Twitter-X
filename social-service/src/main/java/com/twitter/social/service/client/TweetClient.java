package com.twitter.social.service.client;

import com.twitter.social.service.dto.FeedTweetDto;
import com.twitter.social.service.feignDto.TweetResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "tweet-service")
public interface TweetClient {
    @GetMapping("/api/tweets/users")
    List<FeedTweetDto> getTweetsByUserIds(@RequestParam List<Long> userIds);

    @GetMapping("/api/tweets/{tweetId}")
    TweetResponseDto getTweetOwner(@PathVariable Long tweetId);
}