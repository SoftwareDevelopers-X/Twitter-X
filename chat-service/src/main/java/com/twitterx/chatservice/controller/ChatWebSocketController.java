package com.twitterx.chatservice.controller;

import com.twitterx.chatservice.dto.ChatMessageRequest;
import com.twitterx.chatservice.dto.ChatMessageResponse;
import com.twitterx.chatservice.dto.WsEvent;
import com.twitterx.chatservice.enums.WsEventType;
import com.twitterx.chatservice.service.ConversationService;
import com.twitterx.chatservice.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;

/**
 * STOMP destinations (client -> server), all prefixed with /app (see WebSocketConfig):
 *
 *   /app/chat.sendMessage   -> persists + broadcasts a chat message
 *   /app/chat.typing        -> broadcasts "user is typing" (not persisted)
 *   /app/chat.stopTyping    -> broadcasts "user stopped typing"
 *   /app/chat.markRead      -> updates read pointer + broadcasts a read receipt
 *
 * Server -> client broadcasts go out on:
 *   /topic/conversations.{conversationId}   -> all participants subscribe here for messages + events
 *
 * The client subscribes to /topic/conversations.{id} for every conversation it has open,
 * right after CONNECT (typically driven by the conversation list from the REST API).
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private static final String CONVERSATION_TOPIC_PREFIX = "/topic/conversations.";

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;
    private final ConversationService conversationService;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Valid @Payload ChatMessageRequest request, Principal principal) {
        Long senderId = resolveUserId(principal);
        System.out.println("=== Received ChatMessageRequest in WebSocketController: " + request + " from sender: " + senderId + " ===");

        // Never trust the client to say who they are - re-check membership server-side every time.
        conversationService.assertParticipant(request.getConversationId(), senderId);

        ChatMessageResponse saved = messageService.saveMessage(senderId, request);

        String destination = CONVERSATION_TOPIC_PREFIX + request.getConversationId();
        messagingTemplate.convertAndSend(destination, saved);

        System.out.println("=== Broadcast saved message: " + saved.getId() + " to destination: " + destination + " ===");
        log.debug("Message {} broadcast to {}", saved.getId(), destination);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload WsEvent incoming, Principal principal) {
        broadcastEphemeralEvent(incoming, principal, WsEventType.TYPING);
    }

    @MessageMapping("/chat.stopTyping")
    public void stopTyping(@Payload WsEvent incoming, Principal principal) {
        broadcastEphemeralEvent(incoming, principal, WsEventType.STOP_TYPING);
    }

    @MessageMapping("/chat.markRead")
    public void markRead(@Payload WsEvent incoming, Principal principal) {
        Long userId = resolveUserId(principal);
        conversationService.assertParticipant(incoming.getConversationId(), userId);
        conversationService.markRead(incoming.getConversationId(), userId, incoming.getLastReadMessageId());

        WsEvent event = WsEvent.builder()
                .type(WsEventType.READ_RECEIPT)
                .conversationId(incoming.getConversationId())
                .userId(userId)
                .lastReadMessageId(incoming.getLastReadMessageId())
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend(CONVERSATION_TOPIC_PREFIX + incoming.getConversationId(), event);
    }

    private void broadcastEphemeralEvent(WsEvent incoming, Principal principal, WsEventType type) {
        Long userId = resolveUserId(principal);
        conversationService.assertParticipant(incoming.getConversationId(), userId);

        WsEvent event = WsEvent.builder()
                .type(type)
                .conversationId(incoming.getConversationId())
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();

        messagingTemplate.convertAndSend(CONVERSATION_TOPIC_PREFIX + incoming.getConversationId(), event);
    }

    private Long resolveUserId(Principal principal) {
        // principal.getName() returns the userId as a string - see UserPrincipal
        return Long.valueOf(principal.getName());
    }
}
