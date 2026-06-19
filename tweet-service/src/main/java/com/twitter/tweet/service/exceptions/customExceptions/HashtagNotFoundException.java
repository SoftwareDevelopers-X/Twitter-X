package com.twitter.tweet.service.exceptions.customExceptions;

public class HashtagNotFoundException extends RuntimeException {

    public HashtagNotFoundException(String message) {
        super(message);
    }

}
