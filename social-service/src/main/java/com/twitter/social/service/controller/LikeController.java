package com.twitter.social.service.controller;

import com.twitter.social.service.dto.LikeRequestDto;
import com.twitter.social.service.response.ApiResponse;
import com.twitter.social.service.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    @PostMapping
    public ApiResponse<String> likeTweet(@RequestBody LikeRequestDto request) {

        likeService.likeTweet(request);

        return new ApiResponse<>(
                "success",
                "Tweet liked successfully",
                null
        );
    }

    @DeleteMapping
    public String unlikeTweet(@RequestBody LikeRequestDto request) {
        return likeService.unlikeTweet(request);
    }

    @GetMapping("/count/{tweetId}")
    public Long getLikeCount(@PathVariable Long tweetId) {
        return likeService.getLikeCount(tweetId);
    }

    @GetMapping("/status")
    public boolean isTweetLiked(
            @RequestParam Long userId,
            @RequestParam Long tweetId) {

        return likeService.isTweetLiked(userId, tweetId);
    }
}