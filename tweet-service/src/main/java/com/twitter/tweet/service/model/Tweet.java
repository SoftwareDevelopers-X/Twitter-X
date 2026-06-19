package com.twitter.tweet.service.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private Long tweetId;

    private Long userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private Long likeCount = 0L;

    @Builder.Default
    private Long replyCount = 0L;

    @Builder.Default
    private Long retweetCount = 0L;

    @Builder.Default
    private Long viewCount = 0L;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @Builder.Default
    @OneToMany(mappedBy = "tweet", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<TweetMedia> mediaList = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "tweet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TweetHashtag> tweetHashtags = new ArrayList<>();
}
