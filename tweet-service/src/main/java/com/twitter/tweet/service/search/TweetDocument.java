package com.twitter.tweet.service.search;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "tweets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TweetDocument {

    @Id
    private Long tweetId;

    private Long userId;

    private String content;

    private List<String> hashtags;
    private List<String> mediaUrls;

    private String username;

    private String displayName;

    private Long likeCount;

    private Long replyCount;

    private Long retweetCount;

    private Long viewCount;

    private LocalDateTime createdAt;
}