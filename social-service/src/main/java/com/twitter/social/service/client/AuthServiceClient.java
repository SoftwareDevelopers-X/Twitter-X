package com.twitter.social.service.client;

import com.twitter.social.service.feignDto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * !!! VERIFY THIS PATH AGAINST YOUR ACTUAL auth-service CONTROLLER !!!
 *
 * Needed to enrich ProfileResponse with username/displayName, since
 * social-service only stores userId, not identity fields.
 *
 * "auth-service" must match spring.application.name in auth-service's config.
 */
@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/internal/users/{id}")
    UserDto getUserById(@PathVariable("id") Long userId);

    @GetMapping("/api/internal/users/search")
    java.util.List<UserDto> searchUsers(@org.springframework.web.bind.annotation.RequestParam("query") String query);

    @GetMapping("/api/internal/users/username/{username}")
    UserDto getUserByUsername(@PathVariable("username") String username);

}
