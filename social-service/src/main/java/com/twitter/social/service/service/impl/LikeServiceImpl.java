package com.twitter.social.service.service.impl;

import com.twitter.social.service.Model.Like;
import com.twitter.social.service.dto.LikeRequestDto;
import com.twitter.social.service.exception.SocialException;
import com.twitter.social.service.repository.LikeRepository;
import com.twitter.social.service.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeRepository likeRepository;

    @Override
    public String likeTweet(LikeRequestDto request) {

        if (request.getUserId().equals(request.getTweetId())) {
            throw new SocialException("Invalid like request");
        }

        if (likeRepository.existsByUserIdAndTweetId(
                request.getUserId(),
                request.getTweetId())) {

            throw new SocialException("Tweet already liked");
        }

        Like like = Like.builder()
                .userId(request.getUserId())
                .tweetId(request.getTweetId())
                .build();

        likeRepository.save(like);

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

        return "Tweet unliked successfully";
    }

    @Override
    public Long getLikeCount(Long tweetId) {
        return (long) likeRepository.findByTweetId(tweetId).size();
    }

    @Override
    public boolean isTweetLiked(Long userId, Long tweetId) {
        return likeRepository.existsByUserIdAndTweetId(userId, tweetId);
    }
}