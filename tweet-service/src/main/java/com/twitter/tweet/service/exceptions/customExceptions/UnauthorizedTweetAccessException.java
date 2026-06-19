package com.twitter.tweet.service.exceptions.customExceptions;


public class UnauthorizedTweetAccessException extends RuntimeException {
    public UnauthorizedTweetAccessException(String message) {
        super(message);
    }
}