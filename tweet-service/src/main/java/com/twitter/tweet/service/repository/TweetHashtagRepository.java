package com.twitter.tweet.service.repository;

import com.twitter.tweet.service.model.TweetHashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TweetHashtagRepository extends JpaRepository<TweetHashtag, Long> {

    List<TweetHashtag> findByHashtag_Name(String hashtag);
}
