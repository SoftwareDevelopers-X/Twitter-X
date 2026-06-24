package com.twitter.social.service.repository;

import com.twitter.social.service.Model.ReplyRetweet;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReplyRetweetRepository extends JpaRepository<ReplyRetweet, Long> {
    Optional<ReplyRetweet> findByUserIdAndReplyId(Long userId, Long replyId);
    boolean existsByUserIdAndReplyId(Long userId, Long replyId);
}
