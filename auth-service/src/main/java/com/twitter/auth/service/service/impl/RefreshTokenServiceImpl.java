package com.twitter.auth.service.service.impl;

import com.twitter.auth.service.Model.RefreshToken;
import com.twitter.auth.service.Model.User;
import com.twitter.auth.service.exception.InvalidRefreshTokenException;
import com.twitter.auth.service.exception.RefreshTokenExpiredException;
import com.twitter.auth.service.exception.UserNotFoundException;
import com.twitter.auth.service.repository.RefreshTokenRepository;
import com.twitter.auth.service.repository.UserRepository;
import com.twitter.auth.service.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    private final long refreshTokenDuration =
            1000L * 60 * 60 * 24 * 30;

    @Override
    public RefreshToken createRefreshToken(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDuration))
                .build();

        return refreshTokenRepository.save(token);
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken token) {

        if (token.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new RefreshTokenExpiredException(
                    "Refresh token expired"
            );
        }
        return token;
    }

    @Override
    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() ->
                        new InvalidRefreshTokenException(
                                "Invalid refresh token"
                        ));
    }

    @Override
    public void deleteByToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    @Override
    public void logoutAllDevices(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        refreshTokenRepository.deleteByUser(user);
    }

}