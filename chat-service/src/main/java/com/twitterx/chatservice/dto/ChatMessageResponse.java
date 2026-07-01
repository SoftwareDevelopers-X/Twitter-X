package com.twitterx.chatservice.dto;

import com.twitterx.chatservice.enums.MessageStatus;
import com.twitterx.chatservice.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String content;
    private MessageType messageType;
    private MessageStatus status;
    private LocalDateTime createdAt;
    private boolean edited;
    private LocalDateTime editedAt;
    private boolean deleted;
    private java.util.List<ReactionResponse> reactions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReactionResponse {
        private String reaction;
        private Long userId;
    }
}

