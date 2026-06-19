package com.twitter.notification.service.consumer;

import com.twitter.notification.service.dto.NotificationEventDto;
import com.twitter.notification.service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(groupId = "notification-group", topics = "notification-topic")
    public void consume(NotificationEventDto event ){
        System.out.println("----------------------------------------------------------------------------------------------------------------------------");
        System.out.println("received event: " + event.toString());
        notificationService.createNotification(event);
    }
}
