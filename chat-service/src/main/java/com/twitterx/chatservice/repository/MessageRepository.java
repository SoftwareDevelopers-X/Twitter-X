package com.twitterx.chatservice.repository;

import com.twitterx.chatservice.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    Page<Message> findByConversationIdAndDeletedFalseOrderByCreatedAtDesc(Long conversationId, Pageable pageable);

    long countByConversationIdAndIdGreaterThanAndSenderIdNot(Long conversationId, Long lastReadMessageId, Long userId);
}
