package com.twitterx.chatservice.dto;

import com.twitterx.chatservice.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationResponse {
    private Long id;
    private ConversationType type;
    private String name;
    private List<Long> participantIds;
    private String groupImageUrl;
    private ChatMessageResponse lastMessage; // null if no messages yet
    private long unreadCount;
    private LocalDateTime updatedAt;
}

