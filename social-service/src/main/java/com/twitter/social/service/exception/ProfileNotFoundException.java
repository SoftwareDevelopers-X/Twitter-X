package com.twitter.social.service.exception;

/**
 * Thrown when a profile is looked up by userId and doesn't exist (and the
 * lazy-creation fallback wasn't applicable, e.g. for read-only lookups of
 * OTHER users' profiles where we should not silently create profiles for
 * users that may not even exist in auth-service).
 *
 * If SocialException (shown in your Like/LikeService code) already exists
 * with a similar shape, you can make this extend SocialException instead of
 * RuntimeException to keep one exception hierarchy. I extend RuntimeException
 * directly here and add a dedicated handler in GlobalExceptionHandler so it
 * returns 404 instead of being swallowed into the generic 200-with-"error"
 * response your current handleGenericException returns.
 */
public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(String message) {
        super(message);
    }

    public ProfileNotFoundException(Long userId) {
        super("Profile not found for userId: " + userId);
    }
}
