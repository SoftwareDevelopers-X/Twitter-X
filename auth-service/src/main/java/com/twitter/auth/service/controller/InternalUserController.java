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

    @GetMapping("/users/{id}")
    public UserResponse getUserById(@PathVariable Long id) {

        User user = adminService.getUser(id);

        return UserResponse.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .email(user.getEmail())
                .build();
    }
}
