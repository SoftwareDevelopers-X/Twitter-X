package com.twitter.notification.service.service;

import com.twitter.notification.service.Model.Notification;
import com.twitter.notification.service.dto.NotificationEventDto;
import com.twitter.notification.service.dto.NotificationResponse;
import com.twitter.notification.service.exceptions.NotificationNotFoundException;
import com.twitter.notification.service.mapper.NotificationResponseMapper;
import com.twitter.notification.service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

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
                .receiverUserId(event.getReceiverUserId())
                .build();

        repository.save(notification);
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
        return repository
                .findByReceiverUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationResponseMapper::mapToResponse)
                .toList();
    }

    public NotificationResponse markAsRead(Long notificationId) {
        Notification notification = repository.findById(notificationId)
                .orElseThrow(() -> new NotificationNotFoundException("Notification not found"));
        notification.setIsRead(true);
        notification = repository.save(notification);
        return NotificationResponseMapper.mapToResponse(notification);
    }

}
