package com.twitter.auth.service.service.impl;

import com.twitter.auth.service.Enum.AuditAction;
import com.twitter.auth.service.Model.AuditLog;
import com.twitter.auth.service.repository.AuditLogRepository;
import com.twitter.auth.service.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl
        implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    public void log(String email, AuditAction action) {

        AuditLog audit = AuditLog.builder()
                .email(email)
                .action(action)
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(audit);
    }
}