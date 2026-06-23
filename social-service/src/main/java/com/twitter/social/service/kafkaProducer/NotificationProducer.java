package com.twitter.social.service.kafkaProducer;

import com.twitter.events.commonEvents.NotificationEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void send(NotificationEventDto event) {
        System.out.println("------------------------------------------------------------------------------------");
        System.out.println("Sending Event: " + event);

        kafkaTemplate.send("notification-topic", event);
    }
}