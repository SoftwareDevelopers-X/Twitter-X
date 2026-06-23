package com.twitter.events.commonEvents;
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
