package com.twitter.social.service.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookmarks")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookmarkId;

    private Long userId;

    private Long tweetId;

    @CreationTimestamp
    private LocalDateTime bookmarkedAt;
}
