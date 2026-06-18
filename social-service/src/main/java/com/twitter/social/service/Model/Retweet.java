package com.twitter.social.service.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "retweets")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Retweet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long retweetId;

    private Long userId;

    private Long tweetId;

    @CreationTimestamp
    private LocalDateTime retweetedAt;
}