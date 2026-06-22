package com.twitter.auth.service.utility;

import com.twitter.auth.service.Enum.Role;
import com.twitter.auth.service.Model.User;
import com.twitter.auth.service.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminSeeder {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void init() {
        if (!userRepository.existsByEmail("ashraf@gmail.com")) {
            User admin = User.builder()
                    .username("Ashraf")
                    .email("ashraf@gmail.com")
                    .password(passwordEncoder.encode("admin@1234"))
                    .role(Role.ADMIN)
                    .build();

            userRepository.save(admin);
        }
    }
}

