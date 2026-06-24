package com.twitter.social.service.service;

import com.twitter.social.service.Model.Reply;
import com.twitter.social.service.dto.ReplyRequestDto;
import com.twitter.social.service.dto.ReplyResponseDto;

import java.util.List;

public interface ReplyService {

    ReplyResponseDto addReply(ReplyRequestDto request);

    String deleteReply(Long replyId, Long userId, String role);

    List<Reply> getRepliesByTweet(Long tweetId);

    List<ReplyResponseDto> getRepliesByTweet(Long tweetId, Long currentUserId);

    List<Reply> getRepliesByUser(Long userId);

    ReplyResponseDto likeReply(Long replyId, Long userId);

    ReplyResponseDto unlikeReply(Long replyId, Long userId);

    ReplyResponseDto retweetReply(Long replyId, Long userId);

    ReplyResponseDto unretweetReply(Long replyId, Long userId);

    ReplyResponseDto bookmarkReply(Long replyId, Long userId);

    ReplyResponseDto unbookmarkReply(Long replyId, Long userId);

    ReplyResponseDto viewReply(Long replyId);
}