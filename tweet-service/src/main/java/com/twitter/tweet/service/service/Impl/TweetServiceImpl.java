package com.twitter.tweet.service.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.time.Duration;
import java.util.List;
import java.util.Map;
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
    private final RedisTemplate<String,Object> redisTemplate;
    private final ObjectMapper objectMapper;
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
                String redisKey = "hashtag:" + normalizedName;
                Object cached = redisTemplate.opsForValue().get(redisKey);
                Hashtag hashtag = null;
                if (cached instanceof Hashtag h) {
                    hashtag = h;
                } else if (cached instanceof Map<?, ?> map) {
                    hashtag = objectMapper.convertValue(map, Hashtag.class);
                }
                if (hashtag == null) {
                    try {
                        hashtag = hashtagRepository.findByName(normalizedName)
                                .orElseGet(() -> {
                                    Hashtag newHashtag = hashtagRepository.save(
                                            Hashtag.builder()
                                                    .name(normalizedName)
                                                    .build()
                                    );
                                    redisTemplate.opsForValue().set(redisKey, newHashtag);
                                    return newHashtag;
                                });

                        redisTemplate.opsForValue().set(redisKey, hashtag);
                    } catch (DataIntegrityViolationException ex) {

                        hashtag = hashtagRepository.findByName(normalizedName).orElseThrow();
                        redisTemplate.opsForValue().set(redisKey, hashtag);
                    }
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

        // CREATE EVENT
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
        tweetProducer.publishTweetCreatedEvent(event);
        return TweetResponseMapper.mapToResponse(savedTweet);
    }


    @Override
    public TweetResponse getTweet(Long tweetId) {
        Tweet tweet = getTweetOrThrow(tweetId);
        TweetResponse response = TweetResponseMapper.mapToResponse(tweet);
        
        String redisKey = "tweet:" + tweetId;
        try {
            redisTemplate.opsForValue().set(redisKey, response);
        } catch (Exception e) {
            log.error("Failed to update cache for tweet " + tweetId, e);
        }
        
        redisTemplate.opsForValue().increment("tweet:view:" + tweetId);
        try {
            Object viewsObj = redisTemplate.opsForValue().get("tweet:view:" + tweetId);
            if (viewsObj != null) {
                if (viewsObj instanceof Number number) {
                    response.setViewCount(number.longValue());
                } else {
                    response.setViewCount(Long.parseLong(viewsObj.toString()));
                }
            }
        } catch (Exception e) {
            log.error("Failed to fetch view count from Redis for tweet " + tweetId, e);
        }
        
        return response;
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

        TweetResponse response = TweetResponseMapper.mapToResponse(updatedTweet);
        redisTemplate.opsForValue().set("tweet:" + updatedTweet.getTweetId(), response);
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
        Tweet tweet = getTweetOrThrow(tweetId);

        if (!tweet.getUserId().equals(userId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new UnauthorizedTweetAccessException("You are not allowed to delete this tweet");
        }

        tweetRepository.delete(tweet);

        redisTemplate.delete("tweet:" + tweetId);

        redisTemplate.delete("tweet:view:" + tweetId);
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
        return tweetRepository.findById(tweetId)
                .orElseThrow(() -> {
                    log.error("Tweet not found with id {}", tweetId);
                    return new TweetNotFoundException("Tweet not found");
                });
    }

    @Override
    public Page<TweetResponse> getAllTweets(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return tweetRepository.findAll(pageable).map(TweetResponseMapper::mapToResponse);
    }


    @Override
    public List<TweetResponse> getTrendingTweets(String window) {
        String redisKey = "trending:cache:" + window;
        Object cached = redisTemplate.opsForValue().get(redisKey);
        List<TweetResponse> responses = null;
        if (cached instanceof List<?> list) {
            responses = new java.util.ArrayList<>();
            for (Object item : list) {
                responses.add(objectMapper.convertValue(item, TweetResponse.class));
            }
        }
        if (responses == null) {
            List<Tweet> tweets = trendingService.getTrendingTweets(window);
            responses = new java.util.ArrayList<>();
            for (Tweet tweet : tweets) {
                responses.add(TweetResponseMapper.mapToResponse(tweet));
            }
            redisTemplate.opsForValue().set(
                    redisKey,
                    responses,
                    Duration.ofMinutes(10)
            );
        }
        return responses;
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

        return results.stream()
                .map(row -> HashtagResponse.builder()
                        .hashtag((String) row[0])
                        .posts((Long) row[1])
                        .build())
                .toList();

    }


}
