package com.twitter.notification.service.Model;

import com.twitter.events.commonEvents.NotificationType;
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
@Table(name = "notifications", indexes = {@Index(name = "idx_receiver", columnList = "receiverUserId")})
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

    private Long senderUserId;

    private Long receiverUserId;

    private Long tweetId;

    private String message;

    @Builder.Default
    private Boolean isRead = false;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
