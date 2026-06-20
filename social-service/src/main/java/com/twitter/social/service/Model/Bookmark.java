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
@Table(name = "bookmarks", uniqueConstraints = {@UniqueConstraint(columnNames = {"userId", "tweetId"})})
public class Bookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookmarkId;

    private Long userId;

    private Long tweetId;

    @CreationTimestamp
    private LocalDateTime bookmarkedAt;
}
