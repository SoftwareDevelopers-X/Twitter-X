package com.twitter.social.service.service.impl;

import com.twitter.events.commonEvents.TweetLikedEvent;
import com.twitter.events.commonEvents.TweetUnlikedEvent;
import com.twitter.social.service.Enum.NotificationType;
import com.twitter.social.service.Model.Like;
import com.twitter.social.service.client.TweetServiceClient;
import com.twitter.social.service.dto.LikeRequestDto;
import com.twitter.social.service.dto.NotificationEventDto;
import com.twitter.social.service.events.TweetInteractionProducer;
import com.twitter.social.service.exception.SocialException;
import com.twitter.social.service.feignDto.TweetResponse;
import com.twitter.social.service.kafkaProducer.NotificationProducer;
import com.twitter.social.service.repository.LikeRepository;
import com.twitter.social.service.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;

    private final NotificationProducer notificationProducer;

    private final TweetServiceClient tweetServiceClient;
    private final TweetInteractionProducer tweetInteractionProducer;

    @Override
    public String likeTweet(LikeRequestDto request) {

        if (likeRepository.existsByUserIdAndTweetId(request.getUserId(), request.getTweetId())) {
            throw new SocialException("Tweet already liked");
        }
        TweetResponse tweet = tweetServiceClient.getTweet(request.getTweetId());


        Like like = Like.builder()
                .userId(request.getUserId())
                .tweetId(request.getTweetId())
                .build();

        likeRepository.save(like);

              NotificationEventDto event = new NotificationEventDto(
                request.getUserId(),
                      tweet.getUserId(),
                request.getTweetId(),
                "liked your tweet",
                NotificationType.LIKE);

              notificationProducer.send(event);

        TweetLikedEvent tweetLikedEvent= TweetLikedEvent.builder()
                        .tweetId(like.getTweetId())
                        .userId(like.getUserId())
                        .build();
        tweetInteractionProducer.publishTweetLikedEvent(tweetLikedEvent);

        return "Tweet liked successfully";
    }

    @Override
    public String unlikeTweet(LikeRequestDto request) {

        Like like = likeRepository.findByUserIdAndTweetId(
                request.getUserId(),
                request.getTweetId()
        ).orElseThrow(() ->
                new SocialException("Like not found"));

        likeRepository.delete(like);

        TweetUnlikedEvent event = TweetUnlikedEvent.builder()
                        .tweetId(like.getTweetId())
                        .userId(like.getUserId())
                        .build();
        tweetInteractionProducer.publishTweetUnlikedEvent(event);

        return "Tweet unliked successfully";
    }

    @Override
    public Long getLikeCount(Long tweetId) {
        return likeRepository.countByTweetId(tweetId);
    }

    @Override
    public boolean isTweetLiked(Long userId, Long tweetId) {
        return likeRepository.existsByUserIdAndTweetId(userId, tweetId);
    }
}