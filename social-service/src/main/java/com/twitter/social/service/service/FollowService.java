package com.twitter.social.service.service;

import com.twitter.social.service.dto.FollowRequestDto;

import java.util.List;

public interface FollowService {

    String followUser(FollowRequestDto request);
    String unfollowUser(FollowRequestDto request);
    List<Long> getFollowers(Long userId);
    List<Long> getFollowing(Long userId);
    boolean isFollowing(Long followerId, Long followingId);
}