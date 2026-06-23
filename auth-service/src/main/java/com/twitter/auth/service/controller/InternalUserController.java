package com.twitter.auth.service.controller;

import com.twitter.auth.service.Model.User;
import com.twitter.auth.service.dto.UserResponse;
import com.twitter.auth.service.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final AdminService adminService;
    private final com.twitter.auth.service.repository.UserRepository userRepository;

    @GetMapping("/users/{id}")
    public UserResponse getUserById(@PathVariable Long id) {

        User user = adminService.getUser(id);

        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }

    @GetMapping("/users/search")
    public java.util.List<UserResponse> searchUsers(@org.springframework.web.bind.annotation.RequestParam String query) {
        String cleanQuery = query.startsWith("@") ? query.substring(1) : query;
        java.util.List<User> users = userRepository.findByUsernameContainingIgnoreCase(cleanQuery);
        return users.stream()
                .map(user -> UserResponse.builder()
                        .userId(user.getUserId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    @GetMapping("/users/username/{username}")
    public UserResponse getUserByUsername(@PathVariable String username) {
        String cleanUsername = username.startsWith("@") ? username.substring(1) : username;
        User user = userRepository.findByUsernameIgnoreCase(cleanUsername)
                .orElseThrow(() -> new com.twitter.auth.service.exception.UserNotFoundException("User not found"));
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
