package com.twitter.tweet.service.repository;

import com.twitter.tweet.service.model.Hashtag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HashtagRepository extends JpaRepository<Hashtag, Long> {

    Optional<Hashtag> findByName(String name);

    @org.springframework.data.jpa.repository.Query("SELECT h.name, COUNT(th) FROM Hashtag h JOIN h.tweetHashtags th GROUP BY h.name ORDER BY COUNT(th) DESC")
    java.util.List<Object[]> findTrendingHashtags(org.springframework.data.domain.Pageable pageable);
}
