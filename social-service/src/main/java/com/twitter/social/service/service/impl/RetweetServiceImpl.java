package com.twitter.social.service.service.impl;

import com.twitter.social.service.Enum.NotificationType;
import com.twitter.social.service.Model.Retweet;
import com.twitter.social.service.client.TweetClient;
import com.twitter.social.service.dto.NotificationEventDto;
import com.twitter.social.service.dto.RetweetRequestDto;
import com.twitter.social.service.events.TweetInteractionProducer;
import com.twitter.social.service.exception.SocialException;
import com.twitter.social.service.feignDto.TweetResponse;
import com.twitter.social.service.kafkaProducer.NotificationProducer;
import com.twitter.social.service.kafkaProducer.TweetRetweetRemovedEvent;
import com.twitter.social.service.kafkaProducer.TweetRetweetedEvent;
import com.twitter.social.service.repository.RetweetRepository;
import com.twitter.social.service.service.RetweetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RetweetServiceImpl implements RetweetService {

    private final RetweetRepository retweetRepository;

    private final NotificationProducer notificationProducer;

    private final TweetClient tweetClient;
    private final TweetInteractionProducer tweetInteractionProducer;


    @Override
    public String retweet(RetweetRequestDto request) {

        if (retweetRepository.existsByUserIdAndTweetId(request.getUserId(), request.getTweetId())) {
            throw new SocialException("Already retweeted this tweet");
        }

        TweetResponse tweet = tweetClient.getTweet(request.getTweetId());

        Retweet retweet = Retweet.builder()
                .userId(request.getUserId())
                .tweetId(request.getTweetId())
                .build();

        retweetRepository.save(retweet);

        NotificationEventDto event = new NotificationEventDto(
                request.getUserId(),
                tweet.getUserId(),
                request.getTweetId(),
                "retweeted your tweet",
                NotificationType.RETWEET);
        notificationProducer.send(event);

        TweetRetweetedEvent tweetRetweetedEvent= TweetRetweetedEvent.builder()
                        .tweetId(retweet.getTweetId())
                        .userId(retweet.getUserId())
                        .build();

        tweetInteractionProducer.publishTweetRetweetedEvent(tweetRetweetedEvent);

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

        TweetRetweetRemovedEvent event = TweetRetweetRemovedEvent.builder()
                        .tweetId(retweet.getTweetId())
                        .userId(retweet.getUserId())
                        .build();
        tweetInteractionProducer.publishTweetRetweetRemovedEvent(event);
        return "Retweet removed successfully";
    }

    @Override
    public boolean isRetweeted(Long userId, Long tweetId) {
        return retweetRepository.existsByUserIdAndTweetId(userId, tweetId);
    }

    @Override
    public Long getRetweetCount(Long tweetId) {
        return retweetRepository.countByTweetId(tweetId);
    }
}