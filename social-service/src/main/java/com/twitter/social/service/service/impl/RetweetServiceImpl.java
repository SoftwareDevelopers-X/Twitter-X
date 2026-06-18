package com.twitter.social.service.service.impl;

import com.twitter.social.service.Model.Retweet;
import com.twitter.social.service.dto.RetweetRequestDto;
import com.twitter.social.service.exception.SocialException;
import com.twitter.social.service.repository.RetweetRepository;
import com.twitter.social.service.service.RetweetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RetweetServiceImpl implements RetweetService {

    private final RetweetRepository retweetRepository;

    @Override
    public String retweet(RetweetRequestDto request) {

        if (retweetRepository.existsByUserIdAndTweetId(
                request.getUserId(),
                request.getTweetId())) {

            throw new SocialException("Already retweeted this tweet");
        }

        Retweet retweet = Retweet.builder()
                .userId(request.getUserId())
                .tweetId(request.getTweetId())
                .build();

        retweetRepository.save(retweet);

        return "Tweet retweeted successfully";
    }

    @Override
    public String undoRetweet(RetweetRequestDto request) {

        Retweet retweet = retweetRepository.findByUserIdAndTweetId(
                request.getUserId(),
                request.getTweetId()
        ).orElseThrow(() ->
                new SocialException("Retweet not found"));

        retweetRepository.delete(retweet);

        return "Retweet removed successfully";
    }

    @Override
    public boolean isRetweeted(Long userId, Long tweetId) {
        return retweetRepository.existsByUserIdAndTweetId(userId, tweetId);
    }

    @Override
    public Long getRetweetCount(Long tweetId) {
        return (long) Math.toIntExact((long) retweetRepository.findByTweetId(tweetId).size());
    }
}