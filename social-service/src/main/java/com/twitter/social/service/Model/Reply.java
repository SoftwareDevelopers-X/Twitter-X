package com.twitter.social.service.Model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "replies")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Reply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long replyId;

    private Long userId;

    private Long tweetId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @CreationTimestamp
    private LocalDateTime repliedAt;

    private Long parentReplyId;

    @Builder.Default
    private Long likeCount = 0L;

    @Builder.Default
    private Long retweetCount = 0L;

    @Builder.Default
    private Long replyCount = 0L;

    @Builder.Default
    private Long viewCount = 0L;

    @Builder.Default
    private Long bookmarkCount = 0L;
}