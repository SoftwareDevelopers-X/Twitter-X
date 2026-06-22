package com.twitter.social.service.service;

import com.twitter.social.service.Model.Reply;
import com.twitter.social.service.dto.ReplyRequestDto;

import java.util.List;

public interface ReplyService {

    String addReply(ReplyRequestDto request);

    String deleteReply(Long replyId, Long userId, String role);

    List<Reply> getRepliesByTweet(Long tweetId);

    List<Reply> getRepliesByUser(Long userId);
}