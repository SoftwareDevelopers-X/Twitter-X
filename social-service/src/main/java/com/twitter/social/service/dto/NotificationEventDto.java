package com.twitter.social.service.dto;

import com.twitter.social.service.Enum.NotificationType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public record NotificationEventDto(

        Long senderUserId,

        Long receiverUserId,

        Long tweetId,

        String message,

        @Enumerated(EnumType.STRING)
        NotificationType type

) {
}