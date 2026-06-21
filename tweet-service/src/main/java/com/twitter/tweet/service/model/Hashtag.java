package com.twitter.tweet.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hashtags")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Hashtag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long hashtagId;

    @Column(unique = true)
    private String name;

    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "hashtag" )
    private List<TweetHashtag> tweetHashtags = new ArrayList<>();
}
