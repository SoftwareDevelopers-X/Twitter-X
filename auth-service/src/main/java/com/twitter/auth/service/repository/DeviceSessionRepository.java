package com.twitter.auth.service.repository;

import com.twitter.auth.service.Model.DeviceSession;
import com.twitter.auth.service.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DeviceSessionRepository
        extends JpaRepository<DeviceSession, Long> {

    List<DeviceSession> findByUserAndActiveTrue(User user);

    void deleteByRefreshToken(String refreshToken);

    void deleteByUser(User user);
}