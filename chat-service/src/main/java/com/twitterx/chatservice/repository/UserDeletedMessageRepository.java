package com.twitterx.chatservice.repository;

import com.twitterx.chatservice.entity.UserDeletedMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserDeletedMessageRepository extends JpaRepository<UserDeletedMessage, Long> {
    boolean existsByUserIdAndMessageId(Long userId, Long messageId);
    List<UserDeletedMessage> findByUserId(Long userId);
}
