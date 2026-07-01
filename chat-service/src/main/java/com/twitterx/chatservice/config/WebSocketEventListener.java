package com.twitterx.chatservice.config;

import com.twitterx.chatservice.dto.WsEvent;
import com.twitterx.chatservice.entity.UserStatus;
import com.twitterx.chatservice.enums.WsEventType;
import com.twitterx.chatservice.repository.UserStatusRepository;
import com.twitterx.chatservice.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final UserStatusRepository userStatusRepository;
    private final ConversationService conversationService;
    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            Long userId = Long.valueOf(principal.getName());
            log.info("User connected: {}", userId);

            UserStatus status = UserStatus.builder()
                    .userId(userId)
                    .online(true)
                    .lastSeen(LocalDateTime.now())
                    .build();
            userStatusRepository.save(status);

            broadcastStatusChange(userId, true);
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            Long userId = Long.valueOf(principal.getName());
            log.info("User disconnected: {}", userId);

            UserStatus status = UserStatus.builder()
                    .userId(userId)
                    .online(false)
                    .lastSeen(LocalDateTime.now())
                    .build();
            userStatusRepository.save(status);

            broadcastStatusChange(userId, false);
        }
    }

    private void broadcastStatusChange(Long userId, boolean online) {
        try {
            List<Long> activeConversations = conversationService.listConversationsForUser(userId).stream()
                    .map(c -> c.getId())
                    .toList();

            WsEvent statusEvent = WsEvent.builder()
                    .type(WsEventType.USER_STATUS)
                    .userId(userId)
                    .online(online)
                    .lastSeen(LocalDateTime.now())
                    .timestamp(LocalDateTime.now())
                    .build();

            for (Long conversationId : activeConversations) {
                messagingTemplate.convertAndSend("/topic/conversations." + conversationId, statusEvent);
            }
        } catch (Exception e) {
            log.error("Failed to broadcast status change for user {}", userId, e);
        }
    }
}
