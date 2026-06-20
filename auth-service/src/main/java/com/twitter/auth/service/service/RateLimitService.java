package com.twitter.auth.service.service;

public interface RateLimitService {

    void validateLoginAttempt(String key);

    void resetAttempts(String key);

}