package com.twitter.auth.service.service;

public interface TokenBlacklistService {

    void blacklistToken(String token, long expiryTimeMillis);

    boolean isBlacklisted(String token);
}