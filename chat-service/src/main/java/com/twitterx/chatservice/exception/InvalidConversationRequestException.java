package com.twitterx.chatservice.exception;

public class InvalidConversationRequestException extends RuntimeException {
    public InvalidConversationRequestException(String message) {
        super(message);
    }
}
