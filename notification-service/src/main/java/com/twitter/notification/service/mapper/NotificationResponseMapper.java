package com.twitter.notification.service.mapper;

import com.twitter.notification.service.Model.Notification;
import com.twitter.notification.service.dto.NotificationResponse;

public class NotificationResponseMapper {

    public static NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .senderUserId(notification.getSenderUserId())
                .receiverUserId(notification.getReceiverUserId())
                .tweetId(notification.getTweetId())
                .message(notification.getMessage())
                .isRead(notification.getIsRead())
                .type(notification.getType())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
