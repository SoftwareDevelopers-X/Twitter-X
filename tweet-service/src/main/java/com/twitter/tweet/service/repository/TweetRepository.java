package com.twitter.tweet.service.repository;

import com.twitter.tweet.service.model.Tweet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TweetRepository extends JpaRepository<Tweet,Long> {
    List<Tweet> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Tweet> findByUserIdInOrderByCreatedAtDesc(List<Long> userIds);
}
