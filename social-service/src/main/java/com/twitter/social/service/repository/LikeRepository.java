package com.twitter.social.service.repository;

import com.twitter.social.service.Model.Like;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface LikeRepository extends JpaRepository<Like, Long> {

    boolean existsByUserIdAndTweetId(Long userId, Long tweetId);

    Optional<Like> findByUserIdAndTweetId(Long userId, Long tweetId);

    List<Like> findByTweetId(Long tweetId);

    List<Like> findByUserId(Long userId);
}