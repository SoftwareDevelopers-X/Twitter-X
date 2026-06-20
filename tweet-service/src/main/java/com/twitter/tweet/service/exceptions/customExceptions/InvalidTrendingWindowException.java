package com.twitter.tweet.service.exceptions.customExceptions;

public class InvalidTrendingWindowException extends RuntimeException {

    public InvalidTrendingWindowException(String message) {
        super(message);
    }
}