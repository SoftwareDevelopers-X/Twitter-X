package com.twitter.social.service.controller;

import com.twitter.social.service.dto.FollowRequestDto;
import com.twitter.social.service.response.ApiResponse;
import com.twitter.social.service.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/follows")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @PostMapping
    public ApiResponse<String> followUser(@RequestBody FollowRequestDto request) {

        followService.followUser(request);

        return new ApiResponse<>(
                "success",
                "User followed successfully",
                null
        );
    }

    @DeleteMapping
    public ApiResponse<String> unfollowUser(@RequestBody FollowRequestDto request) {

        followService.unfollowUser(request);

        return new ApiResponse<>(
                "success",
                "User unfollowed successfully",
                null
        );
    }

    @GetMapping("/followers/{userId}")
    public ApiResponse<List<Long>> getFollowers(@PathVariable Long userId) {

        return new ApiResponse<>(
                "success",
                "Followers fetched successfully",
                followService.getFollowers(userId)
        );
    }

    @GetMapping("/following/{userId}")
    public ApiResponse<List<Long>> getFollowing(@PathVariable Long userId) {

        return new ApiResponse<>(
                "success",
                "Following fetched successfully",
                followService.getFollowing(userId)
        );
    }

    @GetMapping("/status")
    public ApiResponse<Boolean> isFollowing(
            @RequestParam Long followerId,
            @RequestParam Long followingId) {

        return new ApiResponse<>(
                "success",
                "Follow status fetched successfully",
                followService.isFollowing(followerId, followingId)
        );
    }
}