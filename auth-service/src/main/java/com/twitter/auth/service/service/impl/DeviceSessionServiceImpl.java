package com.twitter.auth.service.service.impl;

import com.twitter.auth.service.Model.DeviceSession;
import com.twitter.auth.service.Model.User;
import com.twitter.auth.service.repository.DeviceSessionRepository;
import com.twitter.auth.service.repository.UserRepository;
import com.twitter.auth.service.service.DeviceSessionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceSessionServiceImpl
        implements DeviceSessionService {

    private final DeviceSessionRepository repository;
    private final UserRepository userRepository;

    @Override
    public void createSession(
            String email,
            String refreshToken,
            String deviceName,
            String ipAddress
    ) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow();

        DeviceSession session =
                DeviceSession.builder()
                        .user(user)
                        .refreshToken(refreshToken)
                        .deviceName(deviceName)
                        .ipAddress(ipAddress)
                        .loginTime(LocalDateTime.now())
                        .active(true)
                        .build();

        repository.save(session);
    }

    @Override
    public List<DeviceSession> getActiveSessions(
            String email
    ) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow();

        return repository.findByUserAndActiveTrue(user);
    }

    @Override
    public void logoutDevice(
            String refreshToken
    ) {

        repository.deleteByRefreshToken(refreshToken);
    }

    @Override
    public void removeAllSessions(String email) {

        User user =
                userRepository.findByEmail(email)
                        .orElseThrow();

        repository.deleteByUser(user);
    }
}