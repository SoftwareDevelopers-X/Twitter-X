package com.twitter.auth.service.service;

import com.twitter.auth.service.Model.AuditLog;
import com.twitter.auth.service.Model.User;

import java.util.List;

public interface AdminService {

    List<User> getAllUsers();

    User getUser(Long userId);

    void lockUser(Long userId);

    void unlockUser(Long userId);

    void disableUser(Long userId);

    void enableUser(Long userId);

    List<AuditLog> getAuditLogs();
}