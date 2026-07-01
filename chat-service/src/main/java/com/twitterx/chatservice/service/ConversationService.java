package com.twitterx.chatservice.service;

import com.twitterx.chatservice.dto.ChatMessageResponse;
import com.twitterx.chatservice.dto.ConversationResponse;
import com.twitterx.chatservice.dto.CreateConversationRequest;
import com.twitterx.chatservice.entity.Conversation;
import com.twitterx.chatservice.entity.ConversationParticipant;
import com.twitterx.chatservice.entity.Message;
import com.twitterx.chatservice.enums.ConversationType;
import com.twitterx.chatservice.exception.ConversationNotFoundException;
import com.twitterx.chatservice.exception.InvalidConversationRequestException;
import com.twitterx.chatservice.exception.NotAParticipantException;
import com.twitterx.chatservice.repository.ConversationParticipantRepository;
import com.twitterx.chatservice.repository.ConversationRepository;
import com.twitterx.chatservice.repository.MessageRepository;
import com.twitterx.chatservice.dto.WsEvent;
import com.twitterx.chatservice.enums.WsEventType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final MessageRepository messageRepository;
    private final SimpMessagingTemplate messagingTemplate;


    @Transactional
    public ConversationResponse createConversation(Long requesterId, CreateConversationRequest request) {

        // de-dupe + ensure requester isn't accidentally listed twice
        Set<Long> otherParticipantIds = new LinkedHashSet<>(request.getParticipantIds());
        otherParticipantIds.remove(requesterId);

        if (otherParticipantIds.isEmpty()) {
            throw new InvalidConversationRequestException("You must include at least one other participant");
        }

        if (request.getType() == ConversationType.ONE_TO_ONE) {
            if (otherParticipantIds.size() != 1) {
                throw new InvalidConversationRequestException("ONE_TO_ONE conversations must have exactly 1 other participant");
            }
            Long otherUserId = otherParticipantIds.iterator().next();
            return findOrCreateDirectConversation(requesterId, otherUserId);
        }

        // GROUP
        if (otherParticipantIds.size() < 2) {
            throw new InvalidConversationRequestException("GROUP conversations need at least 2 other participants");
        }
        if (request.getGroupName() == null || request.getGroupName().isBlank()) {
            throw new InvalidConversationRequestException("groupName is required for GROUP conversations");
        }

        Conversation conversation = Conversation.builder()
                .type(ConversationType.GROUP)
                .name(request.getGroupName())
                .createdBy(requesterId)
                .build();

        conversation = conversationRepository.save(conversation);

        // creator is an admin; everyone else is a regular member
        addParticipant(conversation, requesterId, true);
        for (Long otherId : otherParticipantIds) {
            addParticipant(conversation, otherId, false);
        }

        return toResponse(conversation, requesterId);
    }

    private ConversationResponse findOrCreateDirectConversation(Long userA, Long userB) {
        String directKey = Conversation.buildDirectKey(userA, userB);

        Optional<Conversation> existing = conversationRepository.findByDirectKey(directKey);
        if (existing.isPresent()) {
            return toResponse(existing.get(), userA);
        }

        Conversation conversation = Conversation.builder()
                .type(ConversationType.ONE_TO_ONE)
                .createdBy(userA)
                .directKey(directKey)
                .build();

        conversation = conversationRepository.save(conversation);
        addParticipant(conversation, userA, false);
        addParticipant(conversation, userB, false);

        return toResponse(conversation, userA);
    }

    private void addParticipant(Conversation conversation, Long userId, boolean admin) {
        ConversationParticipant participant = ConversationParticipant.builder()
                .conversation(conversation)
                .userId(userId)
                .admin(admin)
                .build();
        participantRepository.save(participant);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversationsForUser(Long userId) {
        return conversationRepository.findActiveConversationsForUser(userId).stream()
                .map(c -> toResponse(c, userId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Conversation getConversationOrThrow(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));
    }

    /**
     * Verifies the user is an active participant. Used both by REST endpoints
     * and by the STOMP message handler before allowing a send/subscribe.
     */
    @Transactional(readOnly = true)
    public void assertParticipant(Long conversationId, Long userId) {
        boolean active = participantRepository.existsByConversationIdAndUserIdAndLeftAtIsNull(conversationId, userId);
        if (!active) {
            throw new NotAParticipantException(userId, conversationId);
        }
    }

    @Transactional(readOnly = true)
    public List<Long> getActiveParticipantIds(Long conversationId) {
        return participantRepository.findByConversationIdAndLeftAtIsNull(conversationId).stream()
                .map(ConversationParticipant::getUserId)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markRead(Long conversationId, Long userId, Long lastReadMessageId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotAParticipantException(userId, conversationId));
        participant.setLastReadMessageId(lastReadMessageId);
        participantRepository.save(participant);
    }

    private ConversationResponse toResponse(Conversation conversation, Long requesterId) {
        List<Long> participantIds = getActiveParticipantIds(conversation.getId());

        Pageable lastMessagePage = PageRequest.of(0, 1);
        List<Message> lastMessages = messageRepository
                .findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(conversation.getId(), lastMessagePage)
                .getContent();

        ChatMessageResponse lastMessage = lastMessages.isEmpty() ? null : toMessageResponse(lastMessages.get(0));

        long unread = 0;
        Optional<ConversationParticipant> participant =
                participantRepository.findByConversationIdAndUserId(conversation.getId(), requesterId);
        if (participant.isPresent()) {
            Long lastRead = participant.get().getLastReadMessageId();
            if (lastRead != null) {
                unread = messageRepository.countByConversationIdAndIdGreaterThanAndSenderIdNot(
                        conversation.getId(), lastRead, requesterId);
            }
        }

        return ConversationResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .name(conversation.getName())
                .participantIds(participantIds)
                .groupImageUrl(conversation.getGroupImageUrl())
                .lastMessage(lastMessage)
                .unreadCount(unread)
                .updatedAt(conversation.getUpdatedAt())
                .build();

    }

    private ChatMessageResponse toMessageResponse(Message m) {
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

    @Transactional
    public ConversationResponse updateGroupSettings(Long conversationId, Long requesterId, String name, String groupImageUrl) {
        Conversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversationId, requesterId);
        assertAdmin(conversationId, requesterId);

        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalStateException("Only GROUP conversations have settings");
        }

        if (name != null && !name.isBlank()) {
            conversation.setName(name);
        }
        if (groupImageUrl != null) {
            if (groupImageUrl.trim().equalsIgnoreCase("REMOVE") || groupImageUrl.isBlank()) {
                conversation.setGroupImageUrl(null);
            } else {
                conversation.setGroupImageUrl(groupImageUrl);
            }
        }


        conversation = conversationRepository.save(conversation);
        ConversationResponse response = toResponse(conversation, requesterId);

        // Broadcast to WS topic
        WsEvent event = WsEvent.builder()
                .type(WsEventType.GROUP_UPDATE)
                .conversationId(conversationId)
                .userId(requesterId)
                .conversation(response)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/conversations." + conversationId, event);

        return response;
    }

    @Transactional
    public void addParticipant(Long conversationId, Long requesterId, Long userId) {
        Conversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversationId, requesterId);

        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalStateException("Cannot add members to a ONE_TO_ONE conversation");
        }

        Optional<ConversationParticipant> existing = participantRepository.findByConversationIdAndUserId(conversationId, userId);
        if (existing.isPresent()) {
            ConversationParticipant p = existing.get();
            if (p.isActive()) {
                // Already in group
                return;
            } else {
                p.setLeftAt(null);
                p.setJoinedAt(LocalDateTime.now());
                participantRepository.save(p);
            }
        } else {
            addParticipant(conversation, userId, false);
        }

        ConversationResponse response = toResponse(conversation, requesterId);

        // Broadcast USER_JOINED and GROUP_UPDATE
        WsEvent joinedEvent = WsEvent.builder()
                .type(WsEventType.USER_JOINED)
                .conversationId(conversationId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/conversations." + conversationId, joinedEvent);

        WsEvent updateEvent = WsEvent.builder()
                .type(WsEventType.GROUP_UPDATE)
                .conversationId(conversationId)
                .userId(requesterId)
                .conversation(response)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/conversations." + conversationId, updateEvent);
    }

    @Transactional
    public void removeParticipant(Long conversationId, Long requesterId, Long userId) {
        Conversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversationId, requesterId);
        assertAdmin(conversationId, requesterId);

        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalStateException("Cannot remove members from a ONE_TO_ONE conversation");
        }

        ConversationParticipant p = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotAParticipantException(userId, conversationId));

        p.setLeftAt(LocalDateTime.now());
        participantRepository.save(p);

        ConversationResponse response = toResponse(conversation, requesterId);

        // Broadcast USER_LEFT and GROUP_UPDATE
        WsEvent leftEvent = WsEvent.builder()
                .type(WsEventType.USER_LEFT)
                .conversationId(conversationId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/conversations." + conversationId, leftEvent);

        WsEvent updateEvent = WsEvent.builder()
                .type(WsEventType.GROUP_UPDATE)
                .conversationId(conversationId)
                .userId(requesterId)
                .conversation(response)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/conversations." + conversationId, updateEvent);
    }

    @Transactional
    public void leaveGroup(Long conversationId, Long userId) {
        Conversation conversation = getConversationOrThrow(conversationId);
        assertParticipant(conversationId, userId);

        if (conversation.getType() != ConversationType.GROUP) {
            throw new IllegalStateException("Cannot leave a ONE_TO_ONE conversation");
        }

        ConversationParticipant p = participantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotAParticipantException(userId, conversationId));

        p.setLeftAt(LocalDateTime.now());
        participantRepository.save(p);

        ConversationResponse response = toResponse(conversation, userId);

        // Broadcast USER_LEFT and GROUP_UPDATE
        WsEvent leftEvent = WsEvent.builder()
                .type(WsEventType.USER_LEFT)
                .conversationId(conversationId)
                .userId(userId)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/conversations." + conversationId, leftEvent);

        WsEvent updateEvent = WsEvent.builder()
                .type(WsEventType.GROUP_UPDATE)
                .conversationId(conversationId)
                .userId(userId)
                .conversation(response)
                .timestamp(LocalDateTime.now())
                .build();
        messagingTemplate.convertAndSend("/topic/conversations." + conversationId, updateEvent);
    }

    public void assertAdmin(Long conversationId, Long userId) {
        ConversationParticipant participant = participantRepository
                .findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new NotAParticipantException(userId, conversationId));
        if (!participant.isAdmin()) {
            throw new IllegalStateException("User is not an admin of this conversation");
        }
    }
}

