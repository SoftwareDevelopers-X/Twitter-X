package com.twitter.social.service.service.impl;

import com.twitter.social.service.Enum.NotificationType;
import com.twitter.social.service.Model.Reply;
import com.twitter.social.service.client.TweetClient;
import com.twitter.social.service.dto.NotificationEventDto;
import com.twitter.social.service.dto.ReplyRequestDto;
import com.twitter.social.service.exception.SocialException;
import com.twitter.social.service.feignDto.TweetResponseDto;
import com.twitter.social.service.kafkaProducer.NotificationProducer;
import com.twitter.social.service.repository.ReplyRepository;
import com.twitter.social.service.service.ReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReplyServiceImpl implements ReplyService {

    private final ReplyRepository replyRepository;

    private final NotificationProducer notificationProducer;

    private final TweetClient tweetClient;

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

        TweetResponseDto tweetResponseDto = tweetClient.getTweetOwner(request.getTweetId());

        NotificationEventDto event = new NotificationEventDto(
                request.getUserId(),
                tweetResponseDto.getUserId(),
                request.getTweetId(),
                "replied to your tweet",
                NotificationType.RETWEET);

        notificationProducer.send(event);

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