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

    private Integer senderUserId;

    private Integer receiverUserId;

    private Integer tweetId;

    private String message;

    private Boolean isRead;

    @Enumerated(EnumType.STRING)
    private NotificationType type;
}
