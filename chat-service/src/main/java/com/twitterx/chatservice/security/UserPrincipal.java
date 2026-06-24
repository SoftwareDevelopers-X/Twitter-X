package com.twitterx.chatservice.security;

import java.security.Principal;

/**
 * Represents the authenticated user for the duration of a STOMP session.
 * We don't store roles/tokens here - the gateway already did the real auth.
 * This is just a carrier for userId so it survives as the WebSocket session Principal.
 */
public record UserPrincipal(Long userId) implements Principal {

    @Override
    public String getName() {
        return String.valueOf(userId);
    }
}
