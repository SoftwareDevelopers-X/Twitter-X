package com.twitter.notification.service.service;

import com.twitter.events.commonEvents.NotificationEventDto;
import com.twitter.notification.service.Model.Notification;
import com.twitter.notification.service.dto.NotificationResponse;
import com.twitter.notification.service.exceptions.NotificationNotFoundException;
import com.twitter.notification.service.mapper.NotificationResponseMapper;
import com.twitter.notification.service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationResponse createNotification(NotificationEventDto event){
        if (event.getSenderUserId() != null && event.getSenderUserId().equals(event.getReceiverUserId())) {
            return null;
        }
        Notification notification = Notification.builder()
                .senderUserId(event.getSenderUserId())
                .type(event.getType())
                .message(event.getMessage())
                .tweetId(event.getTweetId())
                .receiverUserId(event.getReceiverUserId())
                .build();

        Notification saved = repository.save(notification);
        return NotificationResponseMapper.mapToResponse(saved);
    }

    public NotificationResponse getNotification(Long notificationId) {

        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));

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


    public List<NotificationResponse> getUserNotifications(Long userId) {
        List<Notification> notifications = repository.findByReceiverUserIdOrderByCreatedAtDesc(userId);
        List<NotificationResponse> responses = new java.util.ArrayList<>();
        for (Notification notification : notifications) {
            responses.add(NotificationResponseMapper.mapToResponse(notification));
        }
        return responses;
    }

    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));
        notification.setIsRead(true);
        notification = repository.save(notification);
        return NotificationResponseMapper.mapToResponse(notification);
    }

}
