package com.twitter.tweet.service.service.Impl;

import com.twitter.events.TweetCreatedEvent;
import com.twitter.events.TweetDeletedEvent;
import com.twitter.events.TweetUpdatedEvent;
import com.twitter.tweet.service.client.AuthServiceClient;
import com.twitter.tweet.service.dto.request.MediaRequest;
import com.twitter.tweet.service.dto.request.TweetRequest;
import com.twitter.tweet.service.dto.request.UpdateTweetRequest;
import com.twitter.tweet.service.dto.response.HashtagResponse;
import com.twitter.tweet.service.dto.response.TweetResponse;
import com.twitter.tweet.service.dto.response.UserResponse;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

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
    private final AuthServiceClient authServiceClient;


    @Override
    @Transactional
    public TweetResponse createTweet(TweetRequest request, Long userId) {

        log.info("Creating tweet for user {}", userId);
        Tweet tweet = Tweet.builder()
                .userId(userId)
                .content(request.getContent())
                .build();

        Tweet savedTweet = tweetRepository.save(tweet);

        if (request.getHashtags() != null && !request.getHashtags().isEmpty()) {
            for (String hashtagName : request.getHashtags()) {
                String normalizedName = hashtagName.toLowerCase().trim();
                Hashtag hashtag;
                try {
                    hashtag = hashtagRepository.findByName(normalizedName)
                            .orElseGet(() ->
                                    hashtagRepository.save(
                                            Hashtag.builder()
                                                    .name(normalizedName)
                                                    .build()
                                    )
                            );
                } catch (DataIntegrityViolationException ex) {
                    log.error("Exception occurred in createTweet method {}", ex.getMessage());
                    hashtag = hashtagRepository.findByName(normalizedName).orElseThrow();
                }

                TweetHashtag tweetHashtag = TweetHashtag.builder()
                        .tweet(savedTweet)
                        .hashtag(hashtag)
                        .build();

                tweetHashtagRepository.save(tweetHashtag);
                savedTweet.getTweetHashtags().add(tweetHashtag);
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
                savedTweet.getMediaList().add(media);
            }
        }

        UserResponse user = authServiceClient.getUserById(userId);

        String username = user.getUsername();
        List<String> eventMediaUrls = List.of();
        if (request.getMediaUrls() != null) {
            eventMediaUrls = request.getMediaUrls().stream()
                    .map(MediaRequest::getMediaUrl)
                    .toList();
        }

        TweetCreatedEvent event = this.getTweetCreatedEvent(savedTweet, request,username,eventMediaUrls);
        tweetProducer.publishTweetCreatedEvent(event);
        return TweetResponseMapper.mapToResponse(savedTweet);
    }

    private TweetCreatedEvent getTweetCreatedEvent(Tweet savedTweet, TweetRequest request, String username,List<String> eventMediaUrls){
        TweetCreatedEvent event = TweetCreatedEvent.builder()
                .tweetId(savedTweet.getTweetId())
                .userId(savedTweet.getUserId())
                .content(savedTweet.getContent())
                .hashtags(request.getHashtags())
                .likeCount(savedTweet.getLikeCount())
                .replyCount(savedTweet.getReplyCount())
                .retweetCount(savedTweet.getRetweetCount())
                .viewCount(savedTweet.getViewCount())
                .username(username)
                .createdAt(savedTweet.getCreatedAt())
                .mediaUrls(eventMediaUrls)
                .build();
        return event;
    }


    @Override
    public TweetResponse getTweet(Long tweetId) {
        Tweet tweet = this.getTweetOrThrow(tweetId);
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

        TweetUpdatedEvent event = TweetUpdatedEvent.builder()
                .tweetId(updatedTweet.getTweetId())
                .content(updatedTweet.getContent())
                .hashtags(updatedTweet.getTweetHashtags()
                                .stream()
                                .map(th -> th.getHashtag().getName())
                                .toList()
                )
                .build();

        tweetProducer.publishTweetUpdatedEvent(event);
        return TweetResponseMapper.mapToResponse(updatedTweet);
    }


    @Override
    @Transactional
    public void deleteTweet(Long tweetId, Long userId, String role) {
        log.info("Deleting tweet {} by user {}", tweetId, userId);
        Tweet tweet = this.getTweetOrThrow(tweetId);
        if (!tweet.getUserId().equals(userId) && !"ADMIN".equalsIgnoreCase(role)) {
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
        return tweetRepository.findByUserIdOrderByCreatedAtDesc(userId)
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
        Optional<Tweet> optionalTweet = this.tweetRepository.findById(tweetId);
        if (optionalTweet.isEmpty()) {
            log.error("Tweet not found with id {}", tweetId);
            throw new TweetNotFoundException("Tweet not found");
        }
        return optionalTweet.get();
    }

    @Override
    public Page<TweetResponse> getAllTweets(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return tweetRepository.findAll(pageable).map(TweetResponseMapper::mapToResponse);
    }


    @Override
    public List<TweetResponse> getTrendingTweets(String window) {
        List<Tweet> trendingTweets = trendingService.getTrendingTweets(window);
        return trendingTweets.stream()
                .map(TweetResponseMapper::mapToResponse)
                .toList();
    }

    @Override
    public List<TweetResponse> searchTweets(String keyword) {
        log.info("Searching tweets with keyword {}", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String wildcardPattern = "*" + keyword.trim() + "*";
        return tweetSearchRepository
                .searchAll(wildcardPattern)
                .stream()
                .map(this::mapDocumentToResponse)
                .toList();
    }

    @Override
    public List<TweetResponse> searchSuggestions(String keyword) {
        log.info("Fetching suggestions for {}", keyword);
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }
        String wildcardPattern = "*" + keyword.trim() + "*";
        return tweetSearchRepository
                .searchSuggestions(wildcardPattern)
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

    @Override
    public List<TweetResponse> getTweetsByUserIds(List<Long> userIds) {
        log.info("Fetching tweets for user IDs {}", userIds);
        return tweetRepository.findByUserIdInOrderByCreatedAtDesc(userIds)
                .stream()
                .map(TweetResponseMapper::mapToResponse)
                .toList();
    }

    @Override
    public List<HashtagResponse> getTrendingHashtags() {
        List<Object[]> results = hashtagRepository.findTrendingHashtags(PageRequest.of(0, 20));
        List<HashtagResponse> responses = new ArrayList<>();
        for (Object[] row : results) {
            responses.add(HashtagResponse.builder()
                    .hashtag((String) row[0])
                    .posts((Long) row[1])
                    .build());
        }
        return responses;
    }
}
