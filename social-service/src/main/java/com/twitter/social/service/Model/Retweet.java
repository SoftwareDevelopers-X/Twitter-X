package com.twitter.social.service.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Table(name = "retweets", uniqueConstraints = {@UniqueConstraint(columnNames = {"userId", "tweetId"})})
public class Retweet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long retweetId;

    private Long userId;

    private Long tweetId;

    @CreationTimestamp
    private LocalDateTime retweetedAt;
}