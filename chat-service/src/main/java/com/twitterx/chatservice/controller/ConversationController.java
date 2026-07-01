package com.twitterx.chatservice.controller;

import com.twitterx.chatservice.dto.ChatMessageResponse;
import com.twitterx.chatservice.dto.ConversationResponse;
import com.twitterx.chatservice.dto.CreateConversationRequest;
import com.twitterx.chatservice.dto.UpdateGroupSettingsRequest;
import com.twitterx.chatservice.security.CurrentUserProvider;
import com.twitterx.chatservice.service.ConversationService;
import com.twitterx.chatservice.service.MessageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * All endpoints here are reached through the gateway, e.g.:
 *   POST /chat-service/api/v1/conversations
 * The gateway strips its own prefix and forwards with X-User-Id already set
 * (see GatewayUserHeaderFilter for how it's read).
 */
@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping
    public ResponseEntity<ConversationResponse> createConversation(@Valid @RequestBody CreateConversationRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        ConversationResponse response = conversationService.createConversation(userId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ConversationResponse>> listConversations() {
        Long userId = currentUserProvider.getCurrentUserId();
        return ResponseEntity.ok(conversationService.listConversationsForUser(userId));
    }

    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<Page<ChatMessageResponse>> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {

        Long userId = currentUserProvider.getCurrentUserId();
        conversationService.assertParticipant(conversationId, userId);

        return ResponseEntity.ok(messageService.getMessages(conversationId, userId, page, size));
    }


    @PostMapping("/{conversationId}/read")
    public ResponseEntity<Void> markRead(
            @PathVariable Long conversationId,
            @RequestParam Long lastReadMessageId) {

        Long userId = currentUserProvider.getCurrentUserId();
        conversationService.markRead(conversationId, userId, lastReadMessageId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{conversationId}")
    public ResponseEntity<ConversationResponse> updateGroupSettings(
            @PathVariable Long conversationId,
            @RequestBody UpdateGroupSettingsRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        ConversationResponse response = conversationService.updateGroupSettings(
                conversationId, 
                userId, 
                request.getName(), 
                request.getGroupImageUrl()
        );
        return ResponseEntity.ok(response);
    }


    @PostMapping("/{conversationId}/participants")
    public ResponseEntity<Void> addParticipant(
            @PathVariable Long conversationId,
            @RequestParam Long participantId) {
        Long userId = currentUserProvider.getCurrentUserId();
        conversationService.addParticipant(conversationId, userId, participantId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{conversationId}/participants/{participantId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable Long conversationId,
            @PathVariable Long participantId) {
        Long userId = currentUserProvider.getCurrentUserId();
        conversationService.removeParticipant(conversationId, userId, participantId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{conversationId}/leave")
    public ResponseEntity<Void> leaveGroup(@PathVariable Long conversationId) {
        Long userId = currentUserProvider.getCurrentUserId();
        conversationService.leaveGroup(conversationId, userId);
        return ResponseEntity.ok().build();
    }
}

