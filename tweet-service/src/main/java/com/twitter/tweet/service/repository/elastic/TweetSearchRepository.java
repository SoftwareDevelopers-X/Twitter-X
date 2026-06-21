package com.twitter.tweet.service.repository.elastic;

import com.twitter.tweet.service.search.TweetDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TweetSearchRepository extends ElasticsearchRepository<TweetDocument,Long> {


    @Query("""
{
  "multi_match": {
    "query": "?0",
    "fields": [
      "content",
      "hashtags",
      "username"
    ]
  }
}
""")
    List<TweetDocument> searchAll(String keyword);

    @Query("""
{
  "bool": {
    "should": [
      {
        "wildcard": {
          "content": {
            "value": "*?0*"
          }
        }
      },
      {
        "wildcard": {
          "hashtags": {
            "value": "*?0*"
          }
        }
      },
      {
        "wildcard": {
          "username": {
            "value": "*?0*"
          }
        }
      }
    ]
  }
}
""")
    List<TweetDocument> searchSuggestions(String keyword);
}
