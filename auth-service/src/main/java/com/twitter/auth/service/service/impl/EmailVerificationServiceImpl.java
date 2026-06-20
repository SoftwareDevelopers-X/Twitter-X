package com.twitter.auth.service.service.impl;

import com.twitter.auth.service.Model.EmailVerificationToken;
import com.twitter.auth.service.Model.User;
import com.twitter.auth.service.repository.EmailVerificationTokenRepository;
import com.twitter.auth.service.repository.UserRepository;
import com.twitter.auth.service.service.EmailVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final UserRepository userRepository;

    @Override
    public String createVerificationToken(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken =
                EmailVerificationToken.builder()
                        .token(token)
                        .user(user)
                        .createdAt(LocalDateTime.now())
                        .expiresAt(LocalDateTime.now().plusMinutes(15))
                        .build();

        tokenRepository.save(verificationToken);

        return token;
    }

    @Override
    public void confirmToken(String token) {

        EmailVerificationToken verificationToken =
                tokenRepository.findByToken(token)
                        .orElseThrow(() -> new RuntimeException("Invalid token"));

        if (verificationToken.getConfirmedAt() != null) {
            throw new RuntimeException("Already verified");
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Token expired");
        }

        verificationToken.setConfirmedAt(LocalDateTime.now());

        User user = verificationToken.getUser();
        user.setEnabled(true);

        userRepository.save(user);
        tokenRepository.save(verificationToken);
    }
}