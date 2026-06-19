package com.twitter.tweet.service.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
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
    private Integer tweetId;

    private Integer userId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Builder.Default
    private Integer likeCount = 0;

    @Builder.Default
    private Integer replyCount = 0;

    @Builder.Default
    private Integer retweetCount = 0;

    @Builder.Default
    private Integer viewCount = 0;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "tweet", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<TweetMedia> mediaList;

    @OneToMany(mappedBy = "tweet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TweetHashtag> tweetHashtags;
}
