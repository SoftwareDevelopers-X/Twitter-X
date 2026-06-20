package com.twitter.auth.service.service;

public interface EmailService {

    void sendPasswordResetEmail(
            String toEmail,
            String resetToken
    );
}