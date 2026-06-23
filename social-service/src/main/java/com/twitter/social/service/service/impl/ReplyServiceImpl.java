package com.twitter.social.service.service.impl;

import com.twitter.events.commonEvents.NotificationEventDto;
import com.twitter.events.commonEvents.NotificationType;
import com.twitter.events.commonEvents.TweetRepliedEvent;
import com.twitter.events.commonEvents.TweetReplyDeletedEvent;
import com.twitter.social.service.Model.Reply;
import com.twitter.social.service.client.TweetServiceClient;
import com.twitter.social.service.dto.ReplyRequestDto;
import com.twitter.social.service.events.TweetInteractionProducer;
import com.twitter.social.service.exception.SocialException;
import com.twitter.social.service.feignDto.TweetResponse;
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

    private final TweetServiceClient tweetServiceClient;
    private final TweetInteractionProducer tweetInteractionProducer;


    @Override
    public String addReply(ReplyRequestDto request) {

        if (request.getContent() == null || request.getContent().isEmpty()) {
            throw new SocialException("Reply content cannot be empty");
        }

        TweetResponse tweet = tweetServiceClient.getTweet(request.getTweetId());

        Reply reply = Reply.builder()
                .userId(request.getUserId())
                .tweetId(request.getTweetId())
                .content(request.getContent())
                .build();

        replyRepository.save(reply);

        NotificationEventDto event = new NotificationEventDto(
                request.getUserId(),
                tweet.getUserId(),
                request.getTweetId(),
                "replied to your tweet",
                NotificationType.REPLY);

        notificationProducer.send(event);

        TweetRepliedEvent tweetRepliedEvent = TweetRepliedEvent.builder()
                        .tweetId(reply.getTweetId())
                        .userId(reply.getUserId())
                        .build();
        tweetInteractionProducer.publishTweetRepliedEvent(tweetRepliedEvent);
        return "Reply added successfully";
    }

    @Override
    public String deleteReply(Long replyId, Long userId, String role) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new SocialException("Reply not found"));

        if (!reply.getUserId().equals(userId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new SocialException("You are not allowed to delete this reply");
        }

        replyRepository.delete(reply);

        TweetReplyDeletedEvent event = TweetReplyDeletedEvent.builder()
                .tweetId(reply.getTweetId())
                .userId(reply.getUserId())
                .build();
        tweetInteractionProducer.publishTweetReplyDeletedEvent(event);
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