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
        String cleanQuery;
        if (query.startsWith("@")) {
            cleanQuery = query.substring(1);
        } else {
            cleanQuery = query;
        }
        
        java.util.List<User> users = userRepository.findByUsernameContainingIgnoreCase(cleanQuery);
        java.util.List<UserResponse> responses = new java.util.ArrayList<>();
        for (User user : users) {
            responses.add(UserResponse.builder()
                    .userId(user.getUserId())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .build());
        }
        return responses;
    }

    @GetMapping("/users/username/{username}")
    public UserResponse getUserByUsername(@PathVariable String username) {
        String cleanUsername;
        if (username.startsWith("@")) {
            cleanUsername = username.substring(1);
        } else {
            cleanUsername = username;
        }
        
        User user = userRepository.findByUsernameIgnoreCase(cleanUsername)
                .orElseThrow(() -> new com.twitter.auth.service.exception.UserNotFoundException("User not found"));
        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
