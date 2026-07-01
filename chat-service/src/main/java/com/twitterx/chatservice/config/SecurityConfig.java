package com.twitterx.chatservice.config;

import com.twitterx.chatservice.security.GatewayUserHeaderFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final GatewayUserHeaderFilter gatewayUserHeaderFilter;

    public SecurityConfig(GatewayUserHeaderFilter gatewayUserHeaderFilter) {
        this.gatewayUserHeaderFilter = gatewayUserHeaderFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/ws/**").permitAll() // handshake auth handled by ChatHandshakeInterceptor
                .anyRequest().authenticated()
            )
            .addFilterBefore(gatewayUserHeaderFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
