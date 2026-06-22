package com.twitter.social.service.service.impl;

import com.twitter.social.service.Model.Bookmark;
import com.twitter.social.service.client.TweetServiceClient;
import com.twitter.social.service.dto.BookmarkRequestDto;
import com.twitter.social.service.exception.SocialException;
import com.twitter.social.service.repository.BookmarkRepository;
import com.twitter.social.service.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BookmarkServiceImpl implements BookmarkService {

    private final BookmarkRepository bookmarkRepository;
    private final TweetServiceClient tweetServiceClient;


    @Override
    public String bookmarkTweet(BookmarkRequestDto request) {

        if (bookmarkRepository.existsByUserIdAndTweetId(request.getUserId(), request.getTweetId())) {
            throw new SocialException("Tweet already bookmarked");
        }

        tweetServiceClient.getTweet(request.getTweetId());

        Bookmark bookmark = Bookmark.builder()
                .userId(request.getUserId())
                .tweetId(request.getTweetId())
                .build();

        bookmarkRepository.save(bookmark);

        return "Tweet bookmarked successfully";
    }

    @Override
    public String removeBookmark(BookmarkRequestDto request) {

        Bookmark bookmark = bookmarkRepository.findByUserIdAndTweetId(
                request.getUserId(),
                request.getTweetId()
        ).orElseThrow(() ->
                new SocialException("Bookmark not found"));

        bookmarkRepository.delete(bookmark);

        return "Bookmark removed successfully";
    }

    @Override
    public List<Long> getBookmarkedTweets(Long userId) {

        return bookmarkRepository.findByUserId(userId)
                .stream()
                .map(Bookmark::getTweetId)
                .toList();
    }

    @Override
    public boolean isBookmarked(Long userId, Long tweetId) {
        return bookmarkRepository.existsByUserIdAndTweetId(userId, tweetId);
    }
}