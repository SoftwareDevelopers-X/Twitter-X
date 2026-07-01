package com.twitterx.chatservice.dto;

import com.twitterx.chatservice.enums.WsEventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Generic envelope used for everything that ISN'T a persisted chat message:
 * typing indicators, read receipts, join/leave notifications.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WsEvent {
    private WsEventType type;
    private Long conversationId;
    private Long userId;       // who triggered this event
    private Long lastReadMessageId; // only populated for READ_RECEIPT
    private LocalDateTime timestamp;

    // Additional fields for extended events
    private Long messageId;
    private String content;
    private String reaction;
    private Boolean online;
    private LocalDateTime lastSeen;
    private ChatMessageResponse message;
    private ConversationResponse conversation;
}

