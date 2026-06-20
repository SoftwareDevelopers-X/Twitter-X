package com.twitter.auth.service.constant;

public class AuthConstants {

    public static final int MAX_LOGIN_REQUESTS = 5;

    public static final int LOGIN_RATE_LIMIT_MINUTES = 15;

    public static final int MAX_FAILED_ATTEMPTS = 5;

    public static final int LOCK_DURATION_MINUTES = 15;

    private AuthConstants() {
    }
}