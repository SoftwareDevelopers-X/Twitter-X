package com.twitter.tweet.service.repository;

import com.twitter.tweet.service.model.Tweet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TweetRepository extends JpaRepository<Tweet,Long> {
    List<Tweet> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Tweet> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds);

    @Query("""
            SELECT t
               FROM Tweet t
               WHERE t.createdAt >= :fromTime
               AND (  t.likeCount > 0 OR t.replyCount > 0 OR t.retweetCount > 0)
         """)
    List<Tweet> findByCreatedAtAfter(LocalDateTime fromTime);
}
