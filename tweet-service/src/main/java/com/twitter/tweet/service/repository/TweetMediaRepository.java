package com.twitter.tweet.service.repository;

import com.twitter.tweet.service.model.TweetMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TweetMediaRepository extends JpaRepository<TweetMedia, Long> {
}
