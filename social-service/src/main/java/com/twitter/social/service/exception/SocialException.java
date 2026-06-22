package com.twitter.social.service.exception;

/**
 * NOTE: You already reference SocialException in your existing
 * GlobalExceptionHandler, so this almost certainly already exists in your
 * codebase. This file is included ONLY as a safety net in case it doesn't
 * — if you already have one, DELETE this file, don't have two classes with
 * the same fully-qualified name.
 */
public class SocialException extends RuntimeException {
    public SocialException(String message) {
        super(message);
    }
}
