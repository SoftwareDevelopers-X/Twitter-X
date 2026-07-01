package com.twitterx.chatservice.service;

import com.twitterx.chatservice.dto.ChatMessageRequest;
import com.twitterx.chatservice.dto.ChatMessageResponse;
import com.twitterx.chatservice.dto.WsEvent;
import com.twitterx.chatservice.entity.Conversation;
import com.twitterx.chatservice.entity.Message;
import com.twitterx.chatservice.entity.MessageReaction;
import com.twitterx.chatservice.entity.UserDeletedMessage;
import com.twitterx.chatservice.enums.WsEventType;
import com.twitterx.chatservice.repository.MessageReactionRepository;
import com.twitterx.chatservice.repository.MessageRepository;
import com.twitterx.chatservice.repository.UserDeletedMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final MessageReactionRepository messageReactionRepository;
    private final UserDeletedMessageRepository userDeletedMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String CONVERSATION_TOPIC_PREFIX = "/topic/conversations.";

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
    public Page<ChatMessageResponse> getMessages(Long conversationId, Long userId, int page, int size) {
        Page<Message> messages = messageRepository.findActiveMessagesForUser(
                conversationId, userId, PageRequest.of(page, size));
        return messages.map(this::toResponse);
    }

    @Transactional
    public ChatMessageResponse editMessage(Long userId, Long messageId, String newContent) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new IllegalStateException("Only the sender can edit this message");
        }

        message.setContent(newContent);
        message.setEdited(true);
        message.setEditedAt(LocalDateTime.now());
        message = messageRepository.save(message);

        ChatMessageResponse response = toResponse(message);

        // Broadcast to conversation topic
        WsEvent event = WsEvent.builder()
                .type(WsEventType.MESSAGE_EDITED)
                .conversationId(message.getConversation().getId())
                .userId(userId)
                .messageId(messageId)
                .content(newContent)
                .message(response)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend(CONVERSATION_TOPIC_PREFIX + message.getConversation().getId(), event);

        return response;
    }

    @Transactional
    public void deleteMessageForEveryone(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        if (!message.getSenderId().equals(userId)) {
            throw new IllegalStateException("Only the sender can delete this message");
        }

        message.setDeleted(true);
        message.setContent("This message was deleted");
        messageRepository.save(message);

        // Broadcast delete event
        WsEvent event = WsEvent.builder()
                .type(WsEventType.MESSAGE_DELETED)
                .conversationId(message.getConversation().getId())
                .userId(userId)
                .messageId(messageId)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend(CONVERSATION_TOPIC_PREFIX + message.getConversation().getId(), event);
    }

    @Transactional
    public void deleteMessageForMe(Long userId, Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        conversationService.assertParticipant(message.getConversation().getId(), userId);

        if (!userDeletedMessageRepository.existsByUserIdAndMessageId(userId, messageId)) {
            UserDeletedMessage userDeletedMessage = UserDeletedMessage.builder()
                    .userId(userId)
                    .message(message)
                    .build();
            userDeletedMessageRepository.save(userDeletedMessage);
        }
    }

    @Transactional
    public void addReaction(Long userId, Long messageId, String reaction) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        conversationService.assertParticipant(message.getConversation().getId(), userId);

        if (messageReactionRepository.findByMessageIdAndUserIdAndReaction(messageId, userId, reaction).isEmpty()) {
            MessageReaction messageReaction = MessageReaction.builder()
                    .message(message)
                    .userId(userId)
                    .reaction(reaction)
                    .build();
            messageReactionRepository.save(messageReaction);

            // Broadcast reaction event
            WsEvent event = WsEvent.builder()
                    .type(WsEventType.REACTION_ADD)
                    .conversationId(message.getConversation().getId())
                    .userId(userId)
                    .messageId(messageId)
                    .reaction(reaction)
                    .timestamp(LocalDateTime.now())
                    .build();
            messagingTemplate.convertAndSend(CONVERSATION_TOPIC_PREFIX + message.getConversation().getId(), event);
        }
    }

    @Transactional
    public void removeReaction(Long userId, Long messageId, String reaction) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new IllegalArgumentException("Message not found"));

        conversationService.assertParticipant(message.getConversation().getId(), userId);

        messageReactionRepository.deleteByMessageIdAndUserIdAndReaction(messageId, userId, reaction);

        // Broadcast reaction event
        WsEvent event = WsEvent.builder()
                .type(WsEventType.REACTION_REMOVE)
                .conversationId(message.getConversation().getId())
                .userId(userId)
                .messageId(messageId)
                .reaction(reaction)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend(CONVERSATION_TOPIC_PREFIX + message.getConversation().getId(), event);
    }

    private ChatMessageResponse toResponse(Message m) {
        List<ChatMessageResponse.ReactionResponse> reactions = messageReactionRepository.findByMessageId(m.getId()).stream()
                .map(r -> ChatMessageResponse.ReactionResponse.builder()
                        .reaction(r.getReaction())
                        .userId(r.getUserId())
                        .build())
                .collect(Collectors.toList());

        return ChatMessageResponse.builder()
                .id(m.getId())
                .conversationId(m.getConversation().getId())
                .senderId(m.getSenderId())
                .content(m.getContent())
                .messageType(m.getMessageType())
                .status(m.getStatus())
                .createdAt(m.getCreatedAt())
                .edited(m.isEdited())
                .editedAt(m.getEditedAt())
                .deleted(m.isDeleted())
                .reactions(reactions)
                .build();
    }
}

