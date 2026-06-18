package com.twitter.social.service.repository;

import com.twitter.social.service.Model.Reply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    List<Reply> findByTweetId(Long tweetId);

    List<Reply> findByUserId(Long userId);
}