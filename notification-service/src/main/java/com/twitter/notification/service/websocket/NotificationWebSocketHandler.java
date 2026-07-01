package com.twitter.notification.service.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class NotificationWebSocketHandler extends TextWebSocketHandler {

    private static final Map<Long, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final Map<String, WebSocketSession> allSessions = new ConcurrentHashMap<>();

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri != null) {
            String query = uri.getQuery();
            if (query != null && query.contains("userId=")) {
                try {
                    String userIdStr = query.split("userId=")[1].split("&")[0];
                    Long userId = Long.parseLong(userIdStr);
                    sessions.put(userId, session);
                    session.getAttributes().put("userId", userId);
                    log.info("WebSocket connection established for userId: {}", userId);
                } catch (Exception e) {
                    log.error("Failed to extract userId from query string: {}", query, e);
                }
            }
        }
        allSessions.put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId != null) {
            sessions.remove(userId);
            log.info("WebSocket connection closed for userId: {}", userId);
        }
        allSessions.remove(session.getId());
    }

    public void sendNotification(Long receiverUserId, Object notification) {
        WebSocketSession session = sessions.get(receiverUserId);
        if (session != null && session.isOpen()) {
            try {
                String payload = objectMapper.writeValueAsString(Map.of(
                    "type", "NOTIFICATION",
                    "data", notification
                ));
                session.sendMessage(new TextMessage(payload));
                log.info("Sent real-time notification to userId: {}", receiverUserId);
            } catch (IOException e) {
                log.error("Error sending real-time notification to userId: {}", receiverUserId, e);
            }
        }
    }

    public void broadcast(String type, Object data) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of(
                "type", type,
                "data", data
            ));
        } catch (IOException e) {
            log.error("Error serializing broadcast payload", e);
            return;
        }

        TextMessage message = new TextMessage(payload);
        allSessions.forEach((sessionId, session) -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(message);
                } catch (IOException e) {
                    log.error("Error sending broadcast message to session {}: {}", sessionId, e.getMessage());
                }
            }
        });
    }
}
