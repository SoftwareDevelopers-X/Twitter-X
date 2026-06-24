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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}
