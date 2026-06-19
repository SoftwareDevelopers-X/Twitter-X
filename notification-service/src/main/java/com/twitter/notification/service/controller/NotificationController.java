package com.twitter.notification.service.controller;

import com.twitter.notification.service.dto.NotificationEventDto;
import com.twitter.notification.service.dto.NotificationResponse;
import com.twitter.notification.service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class NotificationController {

    private final NotificationService notificationService;


    @GetMapping("/{notificationId}")
    public NotificationResponse getNotification(
            @PathVariable Long notificationId
    ) {
        return notificationService.getNotification(notificationId);
    }
}
