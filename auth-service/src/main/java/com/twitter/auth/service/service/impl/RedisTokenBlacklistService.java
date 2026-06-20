package com.twitter.auth.service.service.impl;

import com.twitter.auth.service.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RedisTokenBlacklistService implements TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "blacklist:";

    @Override
    public void blacklistToken(String token, long expiryTimeMillis) {

        redisTemplate.opsForValue().set(
                PREFIX + token,
                "blacklisted",
                expiryTimeMillis,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public boolean isBlacklisted(String token) {

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(PREFIX + token)
        );
    }
}