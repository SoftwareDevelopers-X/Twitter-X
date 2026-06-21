package com.twitter.tweet.service.client;

import com.twitter.tweet.service.dto.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/internal/users/{id}")
     public UserResponse getUserById(@PathVariable Long id);
}
