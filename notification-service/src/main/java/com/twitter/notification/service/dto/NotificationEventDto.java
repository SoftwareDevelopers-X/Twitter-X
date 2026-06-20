package com.twitter.notification.service.dto;

import com.twitter.notification.service.Enum.NotificationType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationEventDto {

    private Long senderUserId;
    private Long receiverUserId;
    private Long tweetId;
    private String message;
    private NotificationType type;
}
