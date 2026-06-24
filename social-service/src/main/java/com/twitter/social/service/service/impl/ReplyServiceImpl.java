package com.twitter.social.service.service.impl;

import com.twitter.events.commonEvents.NotificationEventDto;
import com.twitter.events.commonEvents.NotificationType;
import com.twitter.events.commonEvents.TweetRepliedEvent;
import com.twitter.events.commonEvents.TweetReplyDeletedEvent;
import com.twitter.social.service.Model.Reply;
import com.twitter.social.service.Model.ReplyLike;
import com.twitter.social.service.Model.ReplyRetweet;
import com.twitter.social.service.Model.ReplyBookmark;
import com.twitter.social.service.client.TweetServiceClient;
import com.twitter.social.service.dto.ReplyRequestDto;
import com.twitter.social.service.dto.ReplyResponseDto;
import com.twitter.social.service.events.TweetInteractionProducer;
import com.twitter.social.service.exception.SocialException;
import com.twitter.social.service.feignDto.TweetResponse;
import com.twitter.social.service.kafkaProducer.NotificationProducer;
import com.twitter.social.service.repository.ReplyRepository;
import com.twitter.social.service.repository.ReplyLikeRepository;
import com.twitter.social.service.repository.ReplyRetweetRepository;
import com.twitter.social.service.repository.ReplyBookmarkRepository;
import com.twitter.social.service.service.ReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReplyServiceImpl implements ReplyService {

    private final ReplyRepository replyRepository;
    private final ReplyLikeRepository replyLikeRepository;
    private final ReplyRetweetRepository replyRetweetRepository;
    private final ReplyBookmarkRepository replyBookmarkRepository;

    private final NotificationProducer notificationProducer;

    private final TweetServiceClient tweetServiceClient;
    private final TweetInteractionProducer tweetInteractionProducer;


    @Override
    public ReplyResponseDto addReply(ReplyRequestDto request) {

        if (request.getContent() == null || request.getContent().isEmpty()) {
            throw new SocialException("Reply content cannot be empty");
        }

        TweetResponse tweet = tweetServiceClient.getTweet(request.getTweetId());

        Reply reply = Reply.builder()
                .userId(request.getUserId())
                .tweetId(request.getTweetId())
                .content(request.getContent())
                .parentReplyId(request.getParentReplyId())
                .build();

        Reply saved = replyRepository.save(reply);

        if (request.getParentReplyId() != null) {
            Reply parent = replyRepository.findById(request.getParentReplyId())
                    .orElseThrow(() -> new SocialException("Parent reply not found"));
            parent.setReplyCount((parent.getReplyCount() == null ? 0L : parent.getReplyCount()) + 1);
            replyRepository.save(parent);
        }

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
        return getEnrichedReply(saved, request.getUserId());
    }

    @Override
    public String deleteReply(Long replyId, Long userId, String role) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new SocialException("Reply not found"));

        if (!reply.getUserId().equals(userId) && !"ADMIN".equalsIgnoreCase(role)) {
            throw new SocialException("You are not allowed to delete this reply");
        }

        if (reply.getParentReplyId() != null) {
            replyRepository.findById(reply.getParentReplyId()).ifPresent(parent -> {
                parent.setReplyCount(Math.max(0L, (parent.getReplyCount() == null ? 0L : parent.getReplyCount()) - 1));
                replyRepository.save(parent);
            });
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
        List<Reply> replies = replyRepository.findByTweetId(tweetId);
        replies.sort((a, b) -> a.getRepliedAt().compareTo(b.getRepliedAt()));
        return replies;
    }

    @Override
    public List<ReplyResponseDto> getRepliesByTweet(Long tweetId, Long currentUserId) {
        List<Reply> replies = replyRepository.findByTweetId(tweetId);
        replies.sort((a, b) -> a.getRepliedAt().compareTo(b.getRepliedAt()));
        return replies.stream()
                .map(r -> getEnrichedReply(r, currentUserId))
                .toList();
    }

    @Override
    public List<Reply> getRepliesByUser(Long userId) {
        return replyRepository.findByUserId(userId);
    }

    @Override
    public ReplyResponseDto likeReply(Long replyId, Long userId) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new SocialException("Reply not found"));
        if (!replyLikeRepository.existsByUserIdAndReplyId(userId, replyId)) {
            replyLikeRepository.save(ReplyLike.builder()
                    .userId(userId)
                    .replyId(replyId)
                    .build());
            reply.setLikeCount((reply.getLikeCount() == null ? 0L : reply.getLikeCount()) + 1);
            replyRepository.save(reply);
        }
        return getEnrichedReply(reply, userId);
    }

    @Override
    public ReplyResponseDto unlikeReply(Long replyId, Long userId) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new SocialException("Reply not found"));
        replyLikeRepository.findByUserIdAndReplyId(userId, replyId).ifPresent(l -> {
            replyLikeRepository.delete(l);
            reply.setLikeCount(Math.max(0L, (reply.getLikeCount() == null ? 0L : reply.getLikeCount()) - 1));
            replyRepository.save(reply);
        });
        return getEnrichedReply(reply, userId);
    }

    @Override
    public ReplyResponseDto retweetReply(Long replyId, Long userId) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new SocialException("Reply not found"));
        if (!replyRetweetRepository.existsByUserIdAndReplyId(userId, replyId)) {
            replyRetweetRepository.save(ReplyRetweet.builder()
                    .userId(userId)
                    .replyId(replyId)
                    .build());
            reply.setRetweetCount((reply.getRetweetCount() == null ? 0L : reply.getRetweetCount()) + 1);
            replyRepository.save(reply);
        }
        return getEnrichedReply(reply, userId);
    }

    @Override
    public ReplyResponseDto unretweetReply(Long replyId, Long userId) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new SocialException("Reply not found"));
        replyRetweetRepository.findByUserIdAndReplyId(userId, replyId).ifPresent(r -> {
            replyRetweetRepository.delete(r);
            reply.setRetweetCount(Math.max(0L, (reply.getRetweetCount() == null ? 0L : reply.getRetweetCount()) - 1));
            replyRepository.save(reply);
        });
        return getEnrichedReply(reply, userId);
    }

    @Override
    public ReplyResponseDto bookmarkReply(Long replyId, Long userId) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new SocialException("Reply not found"));
        if (!replyBookmarkRepository.existsByUserIdAndReplyId(userId, replyId)) {
            replyBookmarkRepository.save(ReplyBookmark.builder()
                    .userId(userId)
                    .replyId(replyId)
                    .build());
            reply.setBookmarkCount((reply.getBookmarkCount() == null ? 0L : reply.getBookmarkCount()) + 1);
            replyRepository.save(reply);
        }
        return getEnrichedReply(reply, userId);
    }

    @Override
    public ReplyResponseDto unbookmarkReply(Long replyId, Long userId) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new SocialException("Reply not found"));
        replyBookmarkRepository.findByUserIdAndReplyId(userId, replyId).ifPresent(b -> {
            replyBookmarkRepository.delete(b);
            reply.setBookmarkCount(Math.max(0L, (reply.getBookmarkCount() == null ? 0L : reply.getBookmarkCount()) - 1));
            replyRepository.save(reply);
        });
        return getEnrichedReply(reply, userId);
    }

    @Override
    public ReplyResponseDto viewReply(Long replyId) {
        Reply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new SocialException("Reply not found"));
        reply.setViewCount((reply.getViewCount() == null ? 0L : reply.getViewCount()) + 1);
        Reply saved = replyRepository.save(reply);
        return getEnrichedReply(saved, null);
    }

    private ReplyResponseDto getEnrichedReply(Reply r, Long currentUserId) {
        boolean isLiked = false;
        boolean isRetweeted = false;
        boolean isBookmarked = false;
        if (currentUserId != null && currentUserId > 0) {
            isLiked = replyLikeRepository.existsByUserIdAndReplyId(currentUserId, r.getReplyId());
            isRetweeted = replyRetweetRepository.existsByUserIdAndReplyId(currentUserId, r.getReplyId());
            isBookmarked = replyBookmarkRepository.existsByUserIdAndReplyId(currentUserId, r.getReplyId());
        }
        return ReplyResponseDto.builder()
                .replyId(r.getReplyId())
                .userId(r.getUserId())
                .tweetId(r.getTweetId())
                .content(r.getContent())
                .repliedAt(r.getRepliedAt())
                .parentReplyId(r.getParentReplyId())
                .likeCount(r.getLikeCount() == null ? 0L : r.getLikeCount())
                .retweetCount(r.getRetweetCount() == null ? 0L : r.getRetweetCount())
                .replyCount(r.getReplyCount() == null ? 0L : r.getReplyCount())
                .viewCount(r.getViewCount() == null ? 0L : r.getViewCount())
                .bookmarkCount(r.getBookmarkCount() == null ? 0L : r.getBookmarkCount())
                .isLiked(isLiked)
                .isRetweeted(isRetweeted)
                .isBookmarked(isBookmarked)
                .build();
    }
}