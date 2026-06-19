package com.twitter.notification.service.repository;

import com.twitter.notification.service.Model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    List<Notification>
    findByReceiverUserIdOrderByCreatedAtDesc(
            Integer receiverUserId
    );
}
