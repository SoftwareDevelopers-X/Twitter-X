package com.twitter.tweet.service.mapper;

import com.twitter.tweet.service.dto.response.TweetResponse;
import com.twitter.tweet.service.model.Tweet;
import com.twitter.tweet.service.model.TweetMedia;
import com.twitter.tweet.service.search.TweetDocument;

public class TweetResponseMapper {

    public static TweetResponse mapToResponse(Tweet tweet) {
        TweetResponse response = TweetResponse.builder()
                .tweetId(tweet.getTweetId())
                .userId(tweet.getUserId())
                .content(tweet.getContent())
                .likeCount(tweet.getLikeCount())
                .replyCount(tweet.getReplyCount())
                .retweetCount(tweet.getRetweetCount())
                .viewCount(tweet.getViewCount())
                .createdAt(tweet.getCreatedAt())
                .mediaUrls( tweet.getMediaList()
                        .stream()
                        .map(TweetMedia::getMediaUrl)
                        .toList())
                .hashtags(tweet.getTweetHashtags()
                        .stream()
                        .map(x -> x.getHashtag().getName())
                        .toList())
                .build();
        return response;
    }

}
