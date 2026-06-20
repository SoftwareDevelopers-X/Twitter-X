package com.twitter.tweet.service.service.Impl;

import com.twitter.events.TweetCreatedEvent;
import com.twitter.events.TweetDeletedEvent;
import com.twitter.events.TweetUpdatedEvent;
import com.twitter.tweet.service.dto.request.MediaRequest;
import com.twitter.tweet.service.dto.request.TweetRequest;
import com.twitter.tweet.service.dto.request.UpdateTweetRequest;
import com.twitter.tweet.service.dto.response.TweetResponse;
import com.twitter.tweet.service.events.producer.TweetProducer;
import com.twitter.tweet.service.exceptions.customExceptions.TweetNotFoundException;
import com.twitter.tweet.service.exceptions.customExceptions.UnauthorizedTweetAccessException;
import com.twitter.tweet.service.mapper.TweetResponseMapper;
import com.twitter.tweet.service.model.Hashtag;
import com.twitter.tweet.service.model.Tweet;
import com.twitter.tweet.service.model.TweetHashtag;
import com.twitter.tweet.service.model.TweetMedia;
import com.twitter.tweet.service.repository.HashtagRepository;
import com.twitter.tweet.service.repository.TweetHashtagRepository;
import com.twitter.tweet.service.repository.TweetMediaRepository;
import com.twitter.tweet.service.repository.TweetRepository;
import com.twitter.tweet.service.repository.elastic.TweetSearchRepository;
import com.twitter.tweet.service.search.TweetDocument;
import com.twitter.tweet.service.service.TrendingService;
import com.twitter.tweet.service.service.TweetService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class TweetServiceImpl implements TweetService {

    private final TweetRepository tweetRepository;
    private final TweetMediaRepository tweetMediaRepository;
    private final HashtagRepository hashtagRepository;
    private final TweetHashtagRepository tweetHashtagRepository;
    private final TrendingService trendingService;
    private final TweetProducer tweetProducer;
    private final TweetSearchRepository tweetSearchRepository;
    private final RedisTemplate<String,String> redisTemplate;


    @Override
    @Transactional
    public TweetResponse createTweet(TweetRequest request, Long userId) {
        log.info("Creating tweet for user {}", userId);
        Tweet tweet = Tweet.builder()
                .userId(userId)
                .content(request.getContent())
                .build();
        Tweet savedTweet = tweetRepository.save(tweet);
        if(request.getHashtags() != null) {
            for (String hashtagName : request.getHashtags()) {
                Hashtag hashtag = this.hashtagRepository.findByName(hashtagName.toLowerCase().trim())
                        .orElseGet(() -> this.hashtagRepository.save(Hashtag.builder()
                                .name(hashtagName.toLowerCase().trim())
                                .build()));

                TweetHashtag tweetHashtag = TweetHashtag.builder()
                        .tweet(savedTweet)
                        .hashtag(hashtag)
                        .build();
                this.tweetHashtagRepository.save(tweetHashtag);
            }
        }

        if (request.getMediaUrls() != null) {
            for (MediaRequest mediaRequest : request.getMediaUrls()) {
                TweetMedia media = TweetMedia.builder()
                        .mediaUrl(mediaRequest.getMediaUrl())
                        .mediaType(mediaRequest.getMediaType())
                        .tweet(savedTweet)
                        .build();
                tweetMediaRepository.save(media);
            }
        }

        TweetCreatedEvent event = TweetCreatedEvent.builder()
                .tweetId(savedTweet.getTweetId())
                .userId(savedTweet.getUserId())
                .content(savedTweet.getContent())
                .hashtags(request.getHashtags())
                .likeCount(savedTweet.getLikeCount())
                .replyCount(savedTweet.getReplyCount())
                .retweetCount(savedTweet.getRetweetCount())
                .viewCount(savedTweet.getViewCount())
                .createdAt(savedTweet.getCreatedAt())
                .mediaUrls(request.getMediaUrls() == null ? List.of()
                                : request.getMediaUrls()
                                .stream()
                                .map(MediaRequest::getMediaUrl)
                                .toList())
                .build();
        tweetProducer.publishTweetCreatedEvent(event);

        return TweetResponseMapper.mapToResponse(savedTweet);
    }

    @Override
    public TweetResponse getTweet(Long tweetId) {
        Tweet tweet = getTweetOrThrow(tweetId);
        redisTemplate.opsForValue().increment("tweet:view:" + tweetId);
        return TweetResponseMapper.mapToResponse(tweet);
    }

    @Override
    @Transactional
    public TweetResponse updateTweet(Long tweetId, UpdateTweetRequest request, Long userId) {
        log.info("Updating tweet {} by user {}", tweetId, userId);
        Tweet tweet = getTweetOrThrow(tweetId);
        if (!tweet.getUserId().equals(userId)) {
            throw new UnauthorizedTweetAccessException("You are not allowed to update this tweet");
        }
        tweet.setContent(request.getContent());
        Tweet updatedTweet = tweetRepository.save(tweet);

        TweetUpdatedEvent event =
                TweetUpdatedEvent.builder()
                        .tweetId(updatedTweet.getTweetId())
                        .content(updatedTweet.getContent())
                        .hashtags(updatedTweet.getTweetHashtags()
                                        .stream()
                                        .map(th -> th.getHashtag().getName())
                                        .toList())
                        .build();

        tweetProducer.publishTweetUpdatedEvent(event);
        return TweetResponseMapper.mapToResponse(updatedTweet);
    }
    @Override
    @Transactional
    public void deleteTweet(Long tweetId, Long userId) {
        log.info("Deleting tweet {} by user {}", tweetId, userId);
        Tweet tweet = getTweetOrThrow(tweetId);
        if (!tweet.getUserId().equals(userId)) {
            throw new UnauthorizedTweetAccessException("You are not allowed to delete this tweet");
        }

        tweetRepository.delete(tweet);

        TweetDeletedEvent event = TweetDeletedEvent.builder()
                        .tweetId(tweetId)
                        .build();
        tweetProducer.publishTweetDeletedEvent(event);
        log.info("Tweet {} deleted successfully", tweetId);
    }


    @Override
    public List<TweetResponse> getUserTweets(Long userId) {
        log.info("Fetching tweets of user {}", userId);
        return tweetRepository.findByUserId(userId)
                .stream()
                .map(TweetResponseMapper::mapToResponse)
                .toList();
    }

    @Override
    public List<TweetResponse> getTweetsByHashtag(String hashtagName) {
        log.info("Fetching tweets for hashtag {}", hashtagName);
        hashtagName = hashtagName.toLowerCase().trim();
        List<TweetHashtag> tweetHashtags = tweetHashtagRepository.findByHashtag_Name(hashtagName);
        return tweetHashtags.stream()
                .map(TweetHashtag::getTweet)
                .map(TweetResponseMapper::mapToResponse)
                .toList();
    }

    private Tweet getTweetOrThrow(Long tweetId) {
        return tweetRepository.findById(tweetId)
                .orElseThrow(() -> {
                    log.error("Tweet not found with id {}", tweetId);
                    return new TweetNotFoundException("Tweet not found");
                });
    }

    @Override
    public Page<TweetResponse> getAllTweets(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return tweetRepository.findAll(pageable).map(TweetResponseMapper::mapToResponse);
    }

    @Override
    public List<TweetResponse> getTrendingTweets(String window) {
        List<Tweet> tweets = trendingService.getTrendingTweets(window);
        return tweets.stream()
                .map(TweetResponseMapper::mapToResponse)
                .toList();
    }

    @Override
    public List<TweetResponse> searchTweets(String keyword) {
        log.info("Searching tweets with keyword {}", keyword);
        return tweetSearchRepository
                .findByContentContainingIgnoreCase(keyword)
                .stream()
                .map(this::mapDocumentToResponse)
                .toList();
    }

    private TweetResponse mapDocumentToResponse(TweetDocument document) {
        return TweetResponse.builder()
                .tweetId(document.getTweetId())
                .userId(document.getUserId())
                .content(document.getContent())
                .hashtags(document.getHashtags())
                .mediaUrls(document.getMediaUrls())
                .likeCount(document.getLikeCount())
                .replyCount(document.getReplyCount())
                .retweetCount(document.getRetweetCount())
                .viewCount(document.getViewCount())
                .createdAt(document.getCreatedAt())
                .build();
    }

}
