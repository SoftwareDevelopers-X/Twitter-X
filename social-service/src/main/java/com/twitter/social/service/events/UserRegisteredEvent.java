package com.twitter.social.service.events;

import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * !!! THIS MUST MATCH THE EVENT auth-service PUBLISHES, FIELD FOR FIELD !!!
 *
 * Ideally this class actually lives in your `common-events` module and BOTH
 * auth-service (producer) and social-service (consumer) depend on it, so you
 * never get serialization drift between producer/consumer. If common-events
 * already has a UserRegisteredEvent, DELETE this file and import that one
 * instead in ProfileEventListener.
 *
 * If common-events does NOT have it yet, move this class there, and make
 * sure auth-service publishes to topic "user-registered" with this exact
 * shape using a JsonSerializer (or your standard event envelope, if
 * common-events defines one).
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRegisteredEvent implements Serializable {
    private Long userId;
    private String username;
    private String email;
    private LocalDateTime registeredAt;
}
