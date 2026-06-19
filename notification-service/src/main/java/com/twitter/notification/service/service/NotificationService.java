package com.twitter.notification.service.service;

import com.twitter.notification.service.Model.Notification;
import com.twitter.notification.service.dto.NotificationEventDto;
import com.twitter.notification.service.dto.NotificationResponse;
import com.twitter.notification.service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    public void createNotification(NotificationEventDto event){
        Notification notification = Notification.builder()
                .senderUserId(event.getSenderUserId())
                .type(event.getType())
                .message(event.getMessage())
                .tweetId(event.getTweetId())
                .isRead(false)
                .receiverUserId(event.getReceiverUserId())
                .build();

        repository.save(notification);
    }

    public NotificationResponse getNotification(Long notificationId) {

        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

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
