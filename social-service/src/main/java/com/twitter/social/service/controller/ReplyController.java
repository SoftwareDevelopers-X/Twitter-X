package com.twitter.social.service.controller;

import com.twitter.social.service.Model.Reply;
import com.twitter.social.service.dto.ReplyRequestDto;
import com.twitter.social.service.dto.ReplyResponseDto;
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
    public ApiResponse<ReplyResponseDto> addReply(@RequestBody ReplyRequestDto request) {
        ReplyResponseDto response = replyService.addReply(request);
        return new ApiResponse<>(
                "success",
                "Reply added successfully",
                response
        );
    }

    @DeleteMapping("/{replyId}")
    public String deleteReply(@PathVariable Long replyId, @RequestHeader("X-User-Id") Long userId, @RequestHeader("X-Role") String role) {
        return replyService.deleteReply(replyId, userId, role);
    }

    @GetMapping("/tweet/{tweetId}")
    public List<ReplyResponseDto> getRepliesByTweet(
            @PathVariable Long tweetId,
            @RequestHeader(value = "X-User-Id", required = false) Long currentUserId
    ) {
        return replyService.getRepliesByTweet(tweetId, currentUserId);
    }

    @GetMapping("/user/{userId}")
    public List<Reply> getRepliesByUser(@PathVariable Long userId) {
        return replyService.getRepliesByUser(userId);
    }

    @PostMapping("/{replyId}/like")
    public ReplyResponseDto likeReply(@PathVariable Long replyId, @RequestHeader("X-User-Id") Long userId) {
        return replyService.likeReply(replyId, userId);
    }

    @PostMapping("/{replyId}/unlike")
    public ReplyResponseDto unlikeReply(@PathVariable Long replyId, @RequestHeader("X-User-Id") Long userId) {
        return replyService.unlikeReply(replyId, userId);
    }

    @PostMapping("/{replyId}/retweet")
    public ReplyResponseDto retweetReply(@PathVariable Long replyId, @RequestHeader("X-User-Id") Long userId) {
        return replyService.retweetReply(replyId, userId);
    }

    @PostMapping("/{replyId}/unretweet")
    public ReplyResponseDto unretweetReply(@PathVariable Long replyId, @RequestHeader("X-User-Id") Long userId) {
        return replyService.unretweetReply(replyId, userId);
    }

    @PostMapping("/{replyId}/bookmark")
    public ReplyResponseDto bookmarkReply(@PathVariable Long replyId, @RequestHeader("X-User-Id") Long userId) {
        return replyService.bookmarkReply(replyId, userId);
    }

    @PostMapping("/{replyId}/unbookmark")
    public ReplyResponseDto unbookmarkReply(@PathVariable Long replyId, @RequestHeader("X-User-Id") Long userId) {
        return replyService.unbookmarkReply(replyId, userId);
    }

    @PostMapping("/{replyId}/view")
    public ReplyResponseDto viewReply(@PathVariable Long replyId) {
        return replyService.viewReply(replyId);
    }
}