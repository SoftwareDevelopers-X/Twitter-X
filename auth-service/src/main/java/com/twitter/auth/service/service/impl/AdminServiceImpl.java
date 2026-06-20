package com.twitter.auth.service.service.impl;

import com.twitter.auth.service.Model.AuditLog;
import com.twitter.auth.service.Model.User;
import com.twitter.auth.service.exception.UserNotFoundException;
import com.twitter.auth.service.repository.AuditLogRepository;
import com.twitter.auth.service.repository.UserRepository;
import com.twitter.auth.service.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc();
    }

    @Override
    public User getUser(Long userId) {

        return userRepository.findById(userId)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "User not found"
                        ));
    }

    @Override
    public void lockUser(Long userId) {

        User user = getUser(userId);

        user.setAccountLocked(true);

        userRepository.save(user);
    }

    @Override
    public void unlockUser(Long userId) {

        User user = getUser(userId);

        user.setAccountLocked(false);
        user.setFailedAttempts(0);
        user.setLockTime(null);

        userRepository.save(user);
    }

    @Override
    public void disableUser(Long userId) {

        User user = getUser(userId);

        user.setEnabled(false);

        userRepository.save(user);
    }

    @Override
    public void enableUser(Long userId) {

        User user = getUser(userId);

        user.setEnabled(true);

        userRepository.save(user);
    }

    @Override
    public List<AuditLog> getAuditLogs() {
        return auditLogRepository.findAll();
    }
}