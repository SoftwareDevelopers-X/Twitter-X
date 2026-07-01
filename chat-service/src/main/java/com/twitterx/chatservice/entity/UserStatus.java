package com.twitterx.chatservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_statuses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatus {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    @Builder.Default
    private boolean online = false;


    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        if (lastSeen == null) {
            lastSeen = LocalDateTime.now();
        }
    }
}
