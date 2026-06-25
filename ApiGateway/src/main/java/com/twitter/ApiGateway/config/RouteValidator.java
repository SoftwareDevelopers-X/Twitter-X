package com.twitter.ApiGateway.config;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RouteValidator {

    public static final List<String> OPEN_API_ENDPOINTS =
            List.of(
                    "/api/auth/register",
                    "/api/auth/login",
                    "/api/auth/refresh",
                    "/swagger-ui",
                    "/v3/api-docs",
                    "/ws/notifications"
            );

    public boolean isSecured(String path) {
        for (String endpoint : OPEN_API_ENDPOINTS) {
            if (path.startsWith(endpoint)) {
                return false;
            }
        }
        return true;
    }
}