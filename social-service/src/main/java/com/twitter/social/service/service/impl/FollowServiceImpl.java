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
        List<Long> followerIds = new java.util.ArrayList<>();
        for (Follow follow : followers) {
            followerIds.add(follow.getFollowerId());
        }
        return followerIds;
    }

    @Override
    public List<Long> getFollowing(Long userId) {
        List<Follow> following = followRepository.findByFollowerId(userId);
        List<Long> followingIds = new java.util.ArrayList<>();
        for (Follow follow : following) {
            followingIds.add(follow.getFollowingId());
        }
        return followingIds;
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
        List<com.twitter.social.service.Model.Profile> allProfiles = profileRepository.findAll();
        List<Long> followingIds = getFollowing(currentUserId);

        List<Long> suggestions = new java.util.ArrayList<>();
        for (com.twitter.social.service.Model.Profile profile : allProfiles) {
            Long userId = profile.getUserId();
            if (!userId.equals(currentUserId) && !followingIds.contains(userId)) {
                suggestions.add(userId);
                if (suggestions.size() >= 5) {
                    break;
                }
            }
        }
        return suggestions;
    }
}