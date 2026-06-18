package com.twitter.social.service.controller;

import com.twitter.social.service.dto.RetweetRequestDto;
import com.twitter.social.service.response.ApiResponse;
import com.twitter.social.service.service.RetweetService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/retweets")
@RequiredArgsConstructor
public class RetweetController {

    private final RetweetService retweetService;

    @PostMapping
    public ApiResponse<String> retweet(@RequestBody RetweetRequestDto request) {

        retweetService.retweet(request);

        return new ApiResponse<>(
                "success",
                "Tweet retweeted successfully",
                null
        );
    }

    @DeleteMapping
    public String undoRetweet(@RequestBody RetweetRequestDto request) {
        return retweetService.undoRetweet(request);
    }

    @GetMapping("/status")
    public boolean isRetweeted(
            @RequestParam Long userId,
            @RequestParam Long tweetId) {

        return retweetService.isRetweeted(userId, tweetId);
    }

    @GetMapping("/count/{tweetId}")
    public Long getRetweetCount(@PathVariable Long tweetId) {
        return retweetService.getRetweetCount(tweetId);
    }
}