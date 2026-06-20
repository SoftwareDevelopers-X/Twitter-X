package com.twitter.auth.service.service.impl;

import com.twitter.auth.service.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendPasswordResetEmail(
            String toEmail,
            String resetToken
    ) {

        String resetLink =
                "http://localhost:3000/reset-password?token="
                        + resetToken;

        SimpleMailMessage message =
                new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Password Reset Request");

        message.setText(
                "Click the link below to reset your password:\n\n"
                        + resetLink
                        + "\n\nThis link expires in 15 minutes."
        );

        mailSender.send(message);
    }
}