package com.twitter.social.service.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "likes")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Like {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long likeId;

    private Long userId;

    private Long tweetId;

    @CreationTimestamp
    private LocalDateTime likedAt;
}