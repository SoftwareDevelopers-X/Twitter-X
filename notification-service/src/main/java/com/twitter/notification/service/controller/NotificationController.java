package com.twitter.notification.service.controller;

import com.twitter.notification.service.dto.NotificationResponse;
import com.twitter.notification.service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;


    @GetMapping("/{notificationId}")
    public NotificationResponse getNotification(@PathVariable Long notificationId
    ) {
        return notificationService.getNotification(notificationId);
    }

    @PutMapping("/{notificationId}/read")
    public NotificationResponse markAsRead(@PathVariable Long notificationId) {
        return notificationService.markAsRead(notificationId);
    }

// i am adding this as of now, if not required later i will delete it , since i believe twitter needed this
   @GetMapping("/user/{userId}")
   public List<NotificationResponse> getUserNotifications(@PathVariable Long userId){
    return notificationService.getUserNotifications(userId);
}

}
