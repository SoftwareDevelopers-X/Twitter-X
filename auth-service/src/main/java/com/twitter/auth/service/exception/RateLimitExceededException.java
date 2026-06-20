package com.twitter.auth.service.exception;

public class RateLimitExceededException
        extends RuntimeException {

    public RateLimitExceededException(String message) {
        super(message);
    }
}