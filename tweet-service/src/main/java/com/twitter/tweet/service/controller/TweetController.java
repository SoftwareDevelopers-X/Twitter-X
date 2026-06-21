package com.twitter.tweet.service.controller;

import com.twitter.tweet.service.dto.request.TweetRequest;
import com.twitter.tweet.service.dto.request.UpdateTweetRequest;
import com.twitter.tweet.service.dto.response.TweetResponse;
import com.twitter.tweet.service.service.TweetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tweets")
@RequiredArgsConstructor
public class TweetController {

    private final TweetService tweetService;

    @PostMapping
    public ResponseEntity<TweetResponse> createTweet(@RequestBody TweetRequest request, @RequestHeader Long userId) {
        TweetResponse response = this.tweetService.createTweet(request, userId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{tweetId}")
    public ResponseEntity<TweetResponse> getTweet(@PathVariable Long tweetId) {
        return ResponseEntity.ok(this.tweetService.getTweet(tweetId));
    }


    @PutMapping("/{tweetId}")
    public ResponseEntity<TweetResponse> updateTweet(@PathVariable Long tweetId, @RequestBody UpdateTweetRequest request,
            @RequestHeader Long userId) {
        return ResponseEntity.ok(tweetService.updateTweet(tweetId, request, userId));
    }

    @DeleteMapping("/{tweetId}")
    public ResponseEntity<String> deleteTweet(@PathVariable Long tweetId, @RequestHeader Long userId) {
        this.tweetService.deleteTweet(tweetId, userId);
        return ResponseEntity.ok("Tweet deleted successfully");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<TweetResponse>> getUserTweets(@PathVariable Long userId) {
        return ResponseEntity.ok(this.tweetService.getUserTweets(userId));
    }

    @GetMapping("/hashtag/{hashtag}")
    public ResponseEntity<List<TweetResponse>> getTweetsByHashtag(@PathVariable String hashtag) {
        return ResponseEntity.ok(this.tweetService.getTweetsByHashtag(hashtag));
    }

    @GetMapping("/search")
    public ResponseEntity<List<TweetResponse>> searchTweets(@RequestParam String keyword) {
        return ResponseEntity.ok(tweetService.searchTweets(keyword));
    }

    @GetMapping("/suggestions")
    public List<TweetResponse> getSuggestions(@RequestParam String keyword) {
        return tweetService.searchSuggestions(keyword);
    }

    @GetMapping
    public ResponseEntity<Page<TweetResponse>> getAllTweets(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(tweetService.getAllTweets(page, size));
    }

    @GetMapping("/trending")
    public ResponseEntity<List<TweetResponse>> getTrendingTweets(@RequestParam(defaultValue = "24h") String window) {
        return ResponseEntity.ok(tweetService.getTrendingTweets(window));
    }
}
