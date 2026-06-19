package com.twitter.social.service.repository;

import com.twitter.social.service.Model.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserIdAndTweetId(Long userId, Long tweetId);

    Optional<Bookmark> findByUserIdAndTweetId(Long userId, Long tweetId);

    List<Bookmark> findByUserId(Long userId);

    List<Bookmark> findByTweetId(Long tweetId);
}