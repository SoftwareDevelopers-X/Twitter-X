package com.twitter.social.service.repository;

import com.twitter.social.service.Model.ReplyLike;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReplyLikeRepository extends JpaRepository<ReplyLike, Long> {
    Optional<ReplyLike> findByUserIdAndReplyId(Long userId, Long replyId);
    boolean existsByUserIdAndReplyId(Long userId, Long replyId);
}
