package com.twitter.tweet.service.Model;

import jakarta.persistence.*;
import lombok.*;

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
    private Integer hashtagId;

    @Column(unique = true)
    private String name;
}
