package com.twitter.notification.service.consumer;

import com.twitter.events.commonEvents.NotificationEventDto;
import com.twitter.notification.service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    @KafkaListener(groupId = "notification-group", topics = "notification-topic")
    public void consume(NotificationEventDto event ){
        log.info("Received notification event {}", event);
        notificationService.createNotification(event);
    }
}
