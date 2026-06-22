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

        User admin = userRepository.findByEmail("ashraf@gmail.com")
                .orElse(User.builder()
                        .username("Ashraf")
                        .email("ashraf@gmail.com")
                        .role(Role.ADMIN)
                        .build());

        admin.setPassword(passwordEncoder.encode("admin@1234"));
        admin.setEnabled(true);

        userRepository.save(admin);
    }
}

