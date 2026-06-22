package com.twitter.social.service.client;

import com.twitter.social.service.dto.FeedTweetDto;
import com.twitter.social.service.feignDto.TweetDto;
import com.twitter.social.service.feignDto.TweetResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "tweet-service")
public interface TweetServiceClient {

    @GetMapping("/api/tweets/users")
    List<FeedTweetDto> getTweetsByUserIds(@RequestParam List<Long> userIds);

    @GetMapping("/api/tweets/{tweetId}")
    TweetResponse getTweet(@PathVariable Long tweetId);

    @GetMapping("/api/tweets/user/{userId}")
    List<TweetDto> getAllTweetsByUser(@PathVariable("userId") Long userId);


    @GetMapping("/api/tweets/{tweetId}")
    TweetDto getTweetById(@PathVariable("tweetId") Long tweetId);
}