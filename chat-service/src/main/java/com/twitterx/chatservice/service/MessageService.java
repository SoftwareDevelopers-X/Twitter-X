package com.twitterx.chatservice.service;

import com.twitterx.chatservice.dto.ChatMessageRequest;
import com.twitterx.chatservice.dto.ChatMessageResponse;
import com.twitterx.chatservice.entity.Conversation;
import com.twitterx.chatservice.entity.Message;
import com.twitterx.chatservice.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;

    /**
     * Persists a new message. Caller is responsible for having already verified
     * (via ConversationService.assertParticipant) that senderId may post here.
     */
    @Transactional
    public ChatMessageResponse saveMessage(Long senderId, ChatMessageRequest request) {
        Conversation conversation = conversationService.getConversationOrThrow(request.getConversationId());

        Message message = Message.builder()
                .conversation(conversation)
                .senderId(senderId)
                .content(request.getContent())
                .messageType(request.getMessageType())
                .build();

        message = messageRepository.save(message);
        return toResponse(message);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(Long conversationId, int page, int size) {
        Page<Message> messages = messageRepository.findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(
                conversationId, PageRequest.of(page, size));
        return messages.map(this::toResponse);
    }

    private ChatMessageResponse toResponse(Message m) {
        return ChatMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSenderId())
                .content(m.getContent())
                .messageType(m.getMessageType())
                .status(m.getStatus())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
