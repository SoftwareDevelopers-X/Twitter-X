package com.twitter.social.service.service.impl;

import com.twitter.social.service.client.TweetServiceClient;
import com.twitter.social.service.dto.FeedTweetDto;
import com.twitter.social.service.service.FeedService;
import com.twitter.social.service.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {

    private final FollowService followService;
    private final TweetServiceClient tweetServiceClient;


    @Override
    public List<FeedTweetDto> getFeed(Long userId, int page, int size) {
        List<Long> followingUsers = this.followService.getFollowing(userId);
        if (followingUsers.isEmpty()) {
            return new ArrayList<>();
        }
        List<FeedTweetDto> feedTweets = this.tweetServiceClient.getTweetsByUserIds(followingUsers);
        feedTweets.sort((a, b) ->
                b.getCreatedAt().compareTo(a.getCreatedAt()));
        return paginate(feedTweets, page, size);
    }


    private List<FeedTweetDto> paginate(List<FeedTweetDto> list, int page, int size) {
        int start = page * size;
        int end = Math.min(start + size, list.size());
        if (start >= list.size()) {
            return new ArrayList<>();
        }
        return list.subList(start, end);
    }
}