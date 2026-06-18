package com.twitter.social.service.repository;

import com.twitter.social.service.Model.Follow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository
        extends JpaRepository<Follow, Long> {

    boolean existsByFollowerIdAndFollowingId(
            Long followerId,
            Long followingId);

    Optional<Follow> findByFollowerIdAndFollowingId(
            Long followerId,
            Long followingId);

    List<Follow> findByFollowerId(Long followerId);

    List<Follow> findByFollowingId(Long followingId);
}