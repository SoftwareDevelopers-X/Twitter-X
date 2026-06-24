package com.twitterx.chatservice.dto;

import com.twitterx.chatservice.enums.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload the client sends to /app/chat.sendMessage
 * senderId is NOT trusted from this payload - it is taken from the
 * authenticated STOMP session principal (set during the WS handshake).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageRequest {

    @NotNull
    private Long conversationId;

    @NotBlank
    private String content;

    @Builder.Default
    private MessageType messageType = MessageType.TEXT;
}
