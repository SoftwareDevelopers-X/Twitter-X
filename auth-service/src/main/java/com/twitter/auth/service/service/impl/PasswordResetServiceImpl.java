package com.twitter.auth.service.service.impl;

import com.twitter.auth.service.Model.PasswordResetToken;
import com.twitter.auth.service.Model.User;
import com.twitter.auth.service.exception.UserNotFoundException;
import com.twitter.auth.service.repository.PasswordResetTokenRepository;
import com.twitter.auth.service.repository.UserRepository;
import com.twitter.auth.service.service.EmailService;
import com.twitter.auth.service.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Override
    public String createResetToken(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        passwordResetTokenRepository.deleteByUserUserId(
                user.getUserId()
        );

        String token = UUID.randomUUID().toString();

        PasswordResetToken resetToken =
                PasswordResetToken.builder()
                        .token(token)
                        .user(user)
                        .expiryTime(
                                LocalDateTime.now().plusMinutes(15)
                        )
                        .build();

        passwordResetTokenRepository.save(resetToken);

        emailService.sendPasswordResetEmail(
                user.getEmail(),
                token
        );

        return "Password reset email sent";
    }

    @Override
    public void resetPassword(
            String token,
            String newPassword
    ) {

        PasswordResetToken resetToken =
                passwordResetTokenRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Invalid reset token"
                                ));

        if (resetToken.getExpiryTime()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Reset token expired"
            );
        }

        User user = resetToken.getUser();

        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);
    }
}