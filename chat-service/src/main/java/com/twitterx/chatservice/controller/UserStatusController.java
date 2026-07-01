package com.twitterx.chatservice.controller;

import com.twitterx.chatservice.entity.UserStatus;
import com.twitterx.chatservice.repository.UserStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserStatusController {

    private final UserStatusRepository userStatusRepository;

    @GetMapping("/{userId}/status")
    public ResponseEntity<UserStatus> getUserStatus(@PathVariable Long userId) {
        UserStatus status = userStatusRepository.findById(userId)
                .orElseGet(() -> UserStatus.builder()
                        .userId(userId)
                        .online(false)
                        .lastSeen(LocalDateTime.now().minusDays(1))
                        .build());
        return ResponseEntity.ok(status);
    }
}
