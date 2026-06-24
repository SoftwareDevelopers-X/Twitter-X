package com.twitterx.chatservice.repository;

import com.twitterx.chatservice.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByDirectKey(String directKey);

    @Query("""
            SELECT DISTINCT c FROM Conversation c
            JOIN c.participants p
            WHERE p.userId = :userId AND p.leftAt IS NULL
            ORDER BY c.updatedAt DESC
            """)
    List<Conversation> findActiveConversationsForUser(@Param("userId") Long userId);
}
