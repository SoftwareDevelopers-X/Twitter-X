package com.twitter.social.service.events;

import com.twitter.social.service.Model.Profile;
import com.twitter.social.service.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Listens for "user-registered" events published by auth-service and
 * auto-creates an empty Profile row, so every user has a profile the moment
 * they sign up (matches real Twitter — profile exists from account creation,
 * even with empty bio/avatar).
 *
 * !!! VERIFY topic name + consumer group against your Kafka setup !!!
 * "social-service-group" is a guess — check application.yml / your other
 * @KafkaListener usages elsewhere in social-service for the existing
 * group-id convention and reuse it instead of introducing a new one, unless
 * you intentionally want a separate consumer group for profile creation.
 *
 * We also keep a lazy-creation fallback in ProfileServiceImpl.getProfile()
 * (getOrCreateProfile) in case this event is ever missed/late/out of order —
 * Kafka delivery + service startup race conditions happen, so the lazy path
 * makes profile creation self-healing rather than a single point of failure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ProfileEventListener {

    private final ProfileRepository profileRepository;

    @KafkaListener(topics = "user-registered", groupId = "social-service-group")
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("Received user-registered event for userId={}", event.getUserId());

        if (profileRepository.existsByUserId(event.getUserId())) {
            log.warn("Profile already exists for userId={}, skipping creation", event.getUserId());
            return;
        }

        Profile profile = Profile.builder()
                .userId(event.getUserId())
                .isVerified(false)
                .isPrivate(false)
                .build();

        profileRepository.save(profile);
        log.info("Profile auto-created for userId={}", event.getUserId());
    }
}
