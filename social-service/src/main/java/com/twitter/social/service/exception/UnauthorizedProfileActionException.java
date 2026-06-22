package com.twitter.social.service.exception;

/**
 * Thrown when userId in the path/param doesn't match the "current user"
 * trying to update a profile or upload avatar/banner. You said security/API
 * gateway is still under work, so for now the "current user" is just
 * whatever the frontend sends (e.g. a header or request param) — this
 * exception exists so the check is already wired in, and once you add real
 * auth (JWT principal), you just swap where currentUserId comes from in
 * ProfileController, no service-layer changes needed.
 */
public class UnauthorizedProfileActionException extends RuntimeException {
    public UnauthorizedProfileActionException(String message) {
        super(message);
    }
}
