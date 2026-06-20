package com.twitter.auth.service.service;

import com.twitter.auth.service.Enum.AuditAction;

public interface AuditLogService {

    void log(String email, AuditAction action);
}