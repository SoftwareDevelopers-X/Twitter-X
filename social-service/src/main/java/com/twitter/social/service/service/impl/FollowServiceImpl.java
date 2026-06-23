package com.twitter.social.service.service.impl;

import com.twitter.events.commonEvents.NotificationEventDto;
import com.twitter.events.commonEvents.NotificationType;
import com.twitter.social.service.Model.Follow;
import com.twitter.social.service.cache.RedisService;
import com.twitter.social.service.dto.FollowRequestDto;
import com.twitter.social.service.exception.SocialException;
import com.twitter.social.service.kafkaProducer.NotificationProducer;
import com.twitter.social.service.repository.FollowRepository;
import com.twitter.social.service.repository.ProfileRepository;
import com.twitter.social.service.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final NotificationProducer notificationProducer;
    private final RedisService redisService;
    private final ProfileRepository profileRepository;


    @Override
    public String followUser(FollowRequestDto request) {

        if(request.getFollowerId()
                .equals(request.getFollowingId())) {

            throw new SocialException(
                    "User cannot follow himself");
        }

        boolean alreadyFollowing =
                followRepository
                        .existsByFollowerIdAndFollowingId(
                                request.getFollowerId(),
                                request.getFollowingId());

        if(alreadyFollowing) {

            throw new SocialException(
                    "Already following this user");
        }

        Follow follow = Follow.builder()
                .followerId(request.getFollowerId())
                .followingId(request.getFollowingId())
                .build();

        followRepository.save(follow);
        redisService.delete("feed::" + request.getFollowerId());

        NotificationEventDto event = new NotificationEventDto(
                request.getFollowerId(),
                request.getFollowingId(),
                null,
                "following successfully !!",
                NotificationType.FOLLOW);

        notificationProducer.send(event);

        return "User Followed Successfully";
    }

    @Override
    public String unfollowUser(FollowRequestDto request) {

        Follow follow = followRepository
                .findByFollowerIdAndFollowingId(
                        request.getFollowerId(),
                        request.getFollowingId()
                )
                .orElseThrow(() ->
                        new SocialException("Follow relationship not found"));

        followRepository.delete(follow);
        redisService.delete("feed::" + request.getFollowerId());

        return "User Unfollowed Successfully";
    }

    @Override
    public List<Long> getFollowers(Long userId) {

        List<Follow> followers = followRepository.findByFollowingId(userId);

        return followers.stream()
                .map(Follow::getFollowerId)
                .toList();
    }

    @Override
    public List<Long> getFollowing(Long userId) {

        List<Follow> following = followRepository.findByFollowerId(userId);

        return following.stream()
                .map(Follow::getFollowingId)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isFollowing(Long followerId, Long followingId) {

        return followRepository.existsByFollowerIdAndFollowingId(
                followerId,
                followingId
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getFollowSuggestions(Long currentUserId) {
        List<Long> allUserIds = profileRepository.findAll().stream()
                .map(com.twitter.social.service.Model.Profile::getUserId)
                .toList();

        List<Long> followingIds = getFollowing(currentUserId);

        return allUserIds.stream()
                .filter(id -> !id.equals(currentUserId))
                .filter(id -> !followingIds.contains(id))
                .limit(5)
                .toList();
    }
}