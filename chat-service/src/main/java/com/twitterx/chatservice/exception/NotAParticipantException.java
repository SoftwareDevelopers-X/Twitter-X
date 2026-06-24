package com.twitterx.chatservice.exception;

public class NotAParticipantException extends RuntimeException {
    public NotAParticipantException(Long userId, Long conversationId) {
        super("User " + userId + " is not an active participant of conversation " + conversationId);
    }
}
