package com.twitter.tweet.service.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "tweets")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Tweet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tweet_id")
    private Integer tweetId;

    private Integer userId;

    @Column(length = 500)
    private String content;

    private Integer likeCount;

    private Integer replyCount;

    private Integer retweetCount;

    private Integer viewCount;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
