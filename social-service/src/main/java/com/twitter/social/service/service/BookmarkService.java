package com.twitter.social.service.service;

import com.twitter.social.service.dto.BookmarkRequestDto;

import java.util.List;

public interface BookmarkService {

    String bookmarkTweet(BookmarkRequestDto request);

    String removeBookmark(BookmarkRequestDto request);

    List<Long> getBookmarkedTweets(Long userId);

    boolean isBookmarked(Long userId, Long tweetId);
}