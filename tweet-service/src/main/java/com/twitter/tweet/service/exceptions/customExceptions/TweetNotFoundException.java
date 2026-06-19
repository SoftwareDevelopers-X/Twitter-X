package com.twitter.tweet.service.exceptions.customExceptions;

public class TweetNotFoundException extends RuntimeException {
    public TweetNotFoundException(String message) {
        super(message);
    }

}