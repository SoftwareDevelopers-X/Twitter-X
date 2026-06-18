package com.twitter.social.service.controller;

import com.twitter.social.service.dto.BookmarkRequestDto;
import com.twitter.social.service.response.ApiResponse;
import com.twitter.social.service.service.BookmarkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bookmarks")
@RequiredArgsConstructor
public class BookmarkController {

    private final BookmarkService bookmarkService;

    @PostMapping
    public ApiResponse<String> bookmarkTweet(@RequestBody BookmarkRequestDto request) {

        bookmarkService.bookmarkTweet(request);

        return new ApiResponse<>(
                "success",
                "Tweet bookmarked successfully",
                null
        );
    }

    @DeleteMapping
    public String removeBookmark(@RequestBody BookmarkRequestDto request) {
        return bookmarkService.removeBookmark(request);
    }

    @GetMapping("/user/{userId}")
    public List<Long> getBookmarkedTweets(@PathVariable Long userId) {
        return bookmarkService.getBookmarkedTweets(userId);
    }

    @GetMapping("/status")
    public boolean isBookmarked(
            @RequestParam Long userId,
            @RequestParam Long tweetId) {

        return bookmarkService.isBookmarked(userId, tweetId);
    }
}