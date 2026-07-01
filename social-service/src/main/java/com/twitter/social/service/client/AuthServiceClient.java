package com.twitter.social.service.client;

import com.twitter.social.service.feignDto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/internal/users/{id}")
    UserDto getUserById(@PathVariable("id") Long userId);

    @GetMapping("/api/internal/users/search")
    java.util.List<UserDto> searchUsers(@org.springframework.web.bind.annotation.RequestParam("query") String query);

    @GetMapping("/api/internal/users/username/{username}")
    UserDto getUserByUsername(@PathVariable("username") String username);

}
