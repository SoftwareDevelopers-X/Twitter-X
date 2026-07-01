package com.twitterx.chatservice.config;

import com.twitterx.chatservice.dto.WsEvent;
import com.twitterx.chatservice.entity.UserStatus;
import com.twitterx.chatservice.enums.WsEventType;
import com.twitterx.chatservice.repository.UserStatusRepository;
import com.twitterx.chatservice.service.ConversationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;

    @Value("${server.port:8086}")
    private String serverPort;

    private static final String PRESENCE_SET_KEY_PREFIX = "chat:presence:sessions:";
    private static final String INSTANCE_SESSIONS_KEY_PREFIX = "chat:presence:instance-sessions:";
    private static final String SESSION_USER_KEY_PREFIX = "chat:presence:session-user:";

    private String getInstanceId() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName() + ":" + serverPort;
        } catch (Exception e) {
            return "host:" + serverPort;
        }
    }

    @EventListener(ContextRefreshedEvent.class)
    public void handleContextRefreshed() {
        String instanceId = getInstanceId();
        log.info("Cleaning up stale presence sessions for instance: {}", instanceId);
        try {
            String instanceKey = INSTANCE_SESSIONS_KEY_PREFIX + instanceId;
            java.util.Set<String> sessionIds = redisTemplate.opsForSet().members(instanceKey);
            if (sessionIds != null && !sessionIds.isEmpty()) {
                for (String sessionId : sessionIds) {
                    String userKey = SESSION_USER_KEY_PREFIX + sessionId;
                    String userIdStr = redisTemplate.opsForValue().get(userKey);
                    if (userIdStr != null) {
                        Long userId = Long.valueOf(userIdStr);
                        String sessionsKey = PRESENCE_SET_KEY_PREFIX + userId;
                        redisTemplate.opsForSet().remove(sessionsKey, sessionId);
                        
                        Long remaining = redisTemplate.opsForSet().size(sessionsKey);
                        if (remaining == null || remaining == 0) {
                            LocalDateTime disconnectTime = LocalDateTime.now();
                            UserStatus status = userStatusRepository.findById(userId)
                                    .orElse(UserStatus.builder().userId(userId).build());
                            status.setOnline(false);
                            status.setLastSeen(disconnectTime);
                            userStatusRepository.save(status);

                            broadcastStatusChange(userId, false, disconnectTime);
                        }
                    }
                    redisTemplate.delete(userKey);
                }
            }
            redisTemplate.delete(instanceKey);
        } catch (Exception e) {
            log.error("Failed to clean up stale sessions on startup", e);
        }
    }

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            Long userId = Long.valueOf(principal.getName());
            String sessionId = headerAccessor.getSessionId();
            log.info("User connected: {}, sessionId: {}", userId, sessionId);

            if (sessionId != null) {
                String sessionsKey = PRESENCE_SET_KEY_PREFIX + userId;
                String instanceKey = INSTANCE_SESSIONS_KEY_PREFIX + getInstanceId();
                String userKey = SESSION_USER_KEY_PREFIX + sessionId;

                redisTemplate.opsForSet().add(sessionsKey, sessionId);
                redisTemplate.expire(sessionsKey, java.time.Duration.ofDays(1));

                redisTemplate.opsForSet().add(instanceKey, sessionId);
                redisTemplate.expire(instanceKey, java.time.Duration.ofDays(1));

                redisTemplate.opsForValue().set(userKey, userId.toString(), java.time.Duration.ofDays(1));

                Long totalSessions = redisTemplate.opsForSet().size(sessionsKey);
                log.info("User {} total active sessions in Redis: {}", userId, totalSessions);

                if (totalSessions != null && totalSessions == 1) {
                    UserStatus status = userStatusRepository.findById(userId)
                            .orElse(UserStatus.builder().userId(userId).build());
                    status.setOnline(true);
                    userStatusRepository.save(status);

                    broadcastStatusChange(userId, true, status.getLastSeen());
                }
            }
        }
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        if (principal != null) {
            Long userId = Long.valueOf(principal.getName());
            String sessionId = headerAccessor.getSessionId();
            log.info("User disconnected: {}, sessionId: {}", userId, sessionId);

            if (sessionId != null) {
                String sessionsKey = PRESENCE_SET_KEY_PREFIX + userId;
                String instanceKey = INSTANCE_SESSIONS_KEY_PREFIX + getInstanceId();
                String userKey = SESSION_USER_KEY_PREFIX + sessionId;

                redisTemplate.opsForSet().remove(sessionsKey, sessionId);
                redisTemplate.opsForSet().remove(instanceKey, sessionId);
                redisTemplate.delete(userKey);

                Long totalSessions = redisTemplate.opsForSet().size(sessionsKey);
                log.info("User {} remaining active sessions in Redis: {}", userId, totalSessions);

                if (totalSessions == null || totalSessions == 0) {
                    LocalDateTime disconnectTime = LocalDateTime.now();
                    UserStatus status = userStatusRepository.findById(userId)
                            .orElse(UserStatus.builder().userId(userId).build());
                    status.setOnline(false);
                    status.setLastSeen(disconnectTime);
                    userStatusRepository.save(status);

                    broadcastStatusChange(userId, false, disconnectTime);
                }
            }
        }
    }

    private void broadcastStatusChange(Long userId, boolean online, LocalDateTime lastSeen) {
        try {
            List<Long> activeConversations = conversationService.listConversationsForUser(userId).stream()
                    .map(c -> c.getId())
                    .toList();

            WsEvent statusEvent = WsEvent.builder()
                    .type(WsEventType.USER_STATUS)
                    .userId(userId)
                    .online(online)
                    .lastSeen(lastSeen)
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
