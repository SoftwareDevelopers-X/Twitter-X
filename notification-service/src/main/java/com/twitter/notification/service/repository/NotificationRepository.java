package com.twitter.notification.service.repository;

import com.twitter.notification.service.Model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification>
    findByReceiverUserIdOrderByCreatedAtDesc( Long receiverUserId );
}
