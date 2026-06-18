package com.twitter.social.service.controller;

import com.twitter.social.service.Model.Reply;
import com.twitter.social.service.dto.ReplyRequestDto;
import com.twitter.social.service.response.ApiResponse;
import com.twitter.social.service.service.ReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/replies")
@RequiredArgsConstructor
public class ReplyController {

    private final ReplyService replyService;

    @PostMapping
    public ApiResponse<String> addReply(@RequestBody ReplyRequestDto request) {

        replyService.addReply(request);

        return new ApiResponse<>(
                "success",
                "Reply added successfully",
                null
        );
    }

    @DeleteMapping("/{replyId}")
    public String deleteReply(@PathVariable Long replyId) {
        return replyService.deleteReply(replyId);
    }

    @GetMapping("/tweet/{tweetId}")
    public List<Reply> getRepliesByTweet(@PathVariable Long tweetId) {
        return replyService.getRepliesByTweet(tweetId);
    }

    @GetMapping("/user/{userId}")
    public List<Reply> getRepliesByUser(@PathVariable Long userId) {
        return replyService.getRepliesByUser(userId);
    }
}