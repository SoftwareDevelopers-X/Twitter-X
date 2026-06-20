package com.twitter.auth.service.service;

import com.twitter.auth.service.Model.DeviceSession;

import java.util.List;

public interface DeviceSessionService {

    void createSession(
            String email,
            String refreshToken,
            String deviceName,
            String ipAddress
    );

    List<DeviceSession> getActiveSessions(
            String email
    );

    void logoutDevice(
            String refreshToken
    );

    void removeAllSessions(String email);
}