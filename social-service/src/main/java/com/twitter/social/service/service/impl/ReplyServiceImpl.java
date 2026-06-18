package com.twitter.social.service.service.impl;

import com.twitter.social.service.Model.Reply;
import com.twitter.social.service.dto.ReplyRequestDto;
import com.twitter.social.service.exception.SocialException;
import com.twitter.social.service.repository.ReplyRepository;
import com.twitter.social.service.service.ReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReplyServiceImpl implements ReplyService {

    private final ReplyRepository replyRepository;

    @Override
    public String addReply(ReplyRequestDto request) {

        if (request.getContent() == null || request.getContent().isEmpty()) {
            throw new SocialException("Reply content cannot be empty");
        }

        Reply reply = Reply.builder()
                .userId(request.getUserId())
                .tweetId(request.getTweetId())
                .content(request.getContent())
                .build();

        replyRepository.save(reply);

        return "Reply added successfully";
    }

    @Override
    public String deleteReply(Long replyId) {

        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() ->
                        new SocialException("Reply not found"));

        replyRepository.delete(reply);

        return "Reply deleted successfully";
    }

    @Override
    public List<Reply> getRepliesByTweet(Long tweetId) {
        return replyRepository.findByTweetId(tweetId);
    }

    @Override
    public List<Reply> getRepliesByUser(Long userId) {
        return replyRepository.findByUserId(userId);
    }
}