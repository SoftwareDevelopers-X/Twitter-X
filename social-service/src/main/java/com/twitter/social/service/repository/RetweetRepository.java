package com.twitter.social.service.repository;

import com.twitter.social.service.Model.Retweet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetweetRepository extends JpaRepository<Retweet, Long> {

    boolean existsByUserIdAndTweetId(Long userId, Long tweetId);

    Optional<Retweet> findByUserIdAndTweetId(Long userId, Long tweetId);

    long countByTweetId(long tweetId);

    List<Retweet> findByUserId(Long userId);
}