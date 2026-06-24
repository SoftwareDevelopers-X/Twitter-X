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
@Table(name = "reply_bookmarks", uniqueConstraints = {@UniqueConstraint(columnNames = {"userId", "replyId"})})
public class ReplyBookmark {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bookmarkId;

    private Long userId;

    private Long replyId;

    @CreationTimestamp
    private LocalDateTime bookmarkedAt;
}
