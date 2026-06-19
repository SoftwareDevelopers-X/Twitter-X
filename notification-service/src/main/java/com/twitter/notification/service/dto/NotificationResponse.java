package com.twitter.notification.service.dto;

import com.twitter.notification.service.Enum.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {

    private Long notificationId;
    private Long senderUserId;
    private Long receiverUserId;
    private Long tweetId;
    private String message;
    private Boolean isRead;
    private NotificationType type;
    private LocalDateTime createdAt;
}