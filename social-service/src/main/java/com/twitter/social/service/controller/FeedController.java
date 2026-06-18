package com.twitter.social.service.controller;

import com.twitter.social.service.dto.FeedTweetDto;
import com.twitter.social.service.service.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    @GetMapping("/{userId}")
    public List<FeedTweetDto> getFeed(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") Long page,
            @RequestParam(defaultValue = "20") Long size
    ) {
        return feedService.getFeed(userId, page, size);
    }
}