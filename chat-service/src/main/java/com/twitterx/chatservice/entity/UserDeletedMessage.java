package com.twitterx.chatservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_deleted_messages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "message_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDeletedMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id", nullable = false)
    private Message message;

    @Column(name = "deleted_at", nullable = false, updatable = false)
    private LocalDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        this.deletedAt = LocalDateTime.now();
    }
}
