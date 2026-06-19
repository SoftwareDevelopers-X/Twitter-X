package com.twitter.tweet.service.model;

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
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tweet_id")
    private Tweet tweet;

    @ManyToOne
    @JoinColumn(name = "hashtag_id")
    private Hashtag hashtag;

}