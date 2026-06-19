package com.twitter.tweet.service.repository.elastic;

import com.twitter.tweet.service.search.TweetDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TweetSearchRepository extends ElasticsearchRepository<TweetDocument,Long> {
    List<TweetDocument> findByContentContainingIgnoreCase(String keyword);

}
