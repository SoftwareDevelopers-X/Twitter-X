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
  "bool": {
    "should": [
      {
        "wildcard": {
          "content": {
            "value": "?0",
            "case_insensitive": true
          }
        }
      },
      {
        "wildcard": {
          "hashtags": {
            "value": "?0",
            "case_insensitive": true
          }
        }
      },
      {
        "wildcard": {
          "username": {
            "value": "?0",
            "case_insensitive": true
          }
        }
      }
    ]
  }
}
""")
    List<TweetDocument> searchAll(String wildcardPattern);

    @Query("""
{
  "bool": {
    "should": [
      {
        "wildcard": {
          "content": {
            "value": "?0",
            "case_insensitive": true
          }
        }
      },
      {
        "wildcard": {
          "hashtags": {
            "value": "?0",
            "case_insensitive": true
          }
        }
      },
      {
        "wildcard": {
          "username": {
            "value": "?0",
            "case_insensitive": true
          }
        }
      }
    ]
  }
}
""")
    List<TweetDocument> searchSuggestions(String wildcardPattern);
}
