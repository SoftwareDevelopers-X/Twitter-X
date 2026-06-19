package com.twitter.notification.service.controller;

import com.twitter.notification.service.Enum.NotificationType;
import com.twitter.notification.service.dto.NotificationEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/test")
public class TestProducerController {

    private final KafkaTemplate<String, NotificationEventDto> kafkaTemplate;

    @GetMapping("/like")
    public String sendLike(){
        NotificationEventDto event =  new NotificationEventDto(1,
                101,
                5,
                "one user liked your tweet !! ",
                false,
                NotificationType.LIKE);

        kafkaTemplate.send("notification-topic", event);
        return "sent!";
    }
}
