package com.twitterx.chatservice.repository;

import com.twitterx.chatservice.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT m FROM Message m WHERE m.conversation.id = :conversationId " +
            "AND m.deleted = false " +
            "AND NOT EXISTS (SELECT udm FROM UserDeletedMessage udm WHERE udm.message.id = m.id AND udm.userId = :userId) " +
            "ORDER BY m.createdAt DESC")
    Page<Message> findActiveMessagesForUser(
            @org.springframework.data.repository.query.Param("conversationId") Long conversationId,
            @org.springframework.data.repository.query.Param("userId") Long userId,
            Pageable pageable);

    long countByConversationIdAndIdGreaterThanAndSenderIdNot(Long conversationId, Long lastReadMessageId, Long userId);

}
