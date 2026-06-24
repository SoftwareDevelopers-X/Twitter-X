package com.twitterx.chatservice.exception;

public class MissingUserHeaderException extends RuntimeException {
    public MissingUserHeaderException() {
        super("X-User-Id header missing - request did not come through the gateway, or gateway misconfigured");
    }
}
