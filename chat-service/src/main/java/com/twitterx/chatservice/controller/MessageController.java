package com.twitterx.chatservice.controller;

import com.twitterx.chatservice.dto.ChatMessageResponse;
import com.twitterx.chatservice.security.CurrentUserProvider;
import com.twitterx.chatservice.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserProvider currentUserProvider;

    @PutMapping("/{messageId}")
    public ResponseEntity<ChatMessageResponse> editMessage(
            @PathVariable Long messageId,
            @RequestParam String content) {
        Long userId = currentUserProvider.getCurrentUserId();
        ChatMessageResponse response = messageService.editMessage(userId, messageId, content);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{messageId}/everyone")
    public ResponseEntity<Void> deleteForEveryone(@PathVariable Long messageId) {
        Long userId = currentUserProvider.getCurrentUserId();
        messageService.deleteMessageForEveryone(userId, messageId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{messageId}/me")
    public ResponseEntity<Void> deleteForMe(@PathVariable Long messageId) {
        Long userId = currentUserProvider.getCurrentUserId();
        messageService.deleteMessageForMe(userId, messageId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{messageId}/reactions")
    public ResponseEntity<Void> addReaction(
            @PathVariable Long messageId,
            @RequestParam String reaction) {
        Long userId = currentUserProvider.getCurrentUserId();
        messageService.addReaction(userId, messageId, reaction);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{messageId}/reactions")
    public ResponseEntity<Void> removeReaction(
            @PathVariable Long messageId,
            @RequestParam String reaction) {
        Long userId = currentUserProvider.getCurrentUserId();
        messageService.removeReaction(userId, messageId, reaction);
        return ResponseEntity.ok().build();
    }
}
