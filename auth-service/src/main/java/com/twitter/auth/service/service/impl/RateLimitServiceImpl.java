package com.twitter.auth.service.service.impl;

import com.twitter.auth.service.constant.AuthConstants;
import com.twitter.auth.service.exception.RateLimitExceededException;
import com.twitter.auth.service.service.RateLimitService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String PREFIX = "login-rate-limit:";

    @Override
    public void validateLoginAttempt(String key) {

        String redisKey = PREFIX + key;

        Long attempts =
                redisTemplate.opsForValue().increment(redisKey);

        if (attempts != null && attempts == 1) {

            redisTemplate.expire(
                    redisKey,
                    AuthConstants.LOGIN_RATE_LIMIT_MINUTES,
                    TimeUnit.MINUTES
            );
        }

        if (attempts != null &&
                attempts > AuthConstants.MAX_LOGIN_REQUESTS) {

            throw new RateLimitExceededException(
                    "Too many login attempts. Try again later."
            );
        }
    }

    @Override
    public void resetAttempts(String key) {

        String redisKey = PREFIX + key;
        redisTemplate.delete(redisKey);
    }
}