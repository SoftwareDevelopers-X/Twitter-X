package com.twitter.tweet.service.Model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tweet_hashtags")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TweetHashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private Integer tweetId;

    private Integer hashtagId;
}