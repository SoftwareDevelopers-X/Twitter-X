package com.twitter.notification.service.dto;

import com.twitter.notification.service.Enum.NotificationType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class NotificationEventDto {

    private Long senderUserId;

    private Long receiverUserId;

    private Long tweetId;

    private String message;

    @Enumerated(EnumType.STRING)
    private NotificationType type;
}
