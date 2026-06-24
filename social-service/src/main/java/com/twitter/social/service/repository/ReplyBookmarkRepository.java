package com.twitter.social.service.repository;

import com.twitter.social.service.Model.ReplyBookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReplyBookmarkRepository extends JpaRepository<ReplyBookmark, Long> {
    Optional<ReplyBookmark> findByUserIdAndReplyId(Long userId, Long replyId);
    boolean existsByUserIdAndReplyId(Long userId, Long replyId);
}
