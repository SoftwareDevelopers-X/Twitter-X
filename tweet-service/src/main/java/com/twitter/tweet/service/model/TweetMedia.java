package com.twitter.tweet.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.twitter.tweet.service.enums.MediaType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "tweet_media")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class TweetMedia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long mediaId;

    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @JoinColumn(name = "tweet_id")
    private Tweet tweet;
}