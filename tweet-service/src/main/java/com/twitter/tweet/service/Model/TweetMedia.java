package com.twitter.tweet.service.Model;

import com.twitter.tweet.service.Enum.MediaType;
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
    private Integer mediaId;

    private String mediaUrl;

    @Enumerated(EnumType.STRING)
    private MediaType mediaType;

    @ManyToOne
    @JoinColumn(name = "tweet_id")
    private Tweet tweet;
}