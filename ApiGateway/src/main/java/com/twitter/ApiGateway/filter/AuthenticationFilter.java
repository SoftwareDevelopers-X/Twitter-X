package com.twitter.ApiGateway.filter;

import com.twitter.ApiGateway.config.RouteValidator;
import com.twitter.ApiGateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter {

    private final RouteValidator routeValidator;
    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(
            ServerWebExchange exchange,
            GatewayFilterChain chain) {

        String path = exchange.getRequest()
                .getURI()
                .getPath();

        // Skip public APIs
        if (!routeValidator.isSecured(path)) {
            return chain.filter(exchange);
        }

        // Get Authorization header
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null) {
            throw new RuntimeException("Missing Authorization Header");
        }

        if (!authHeader.startsWith("Bearer ")) {
            throw new RuntimeException("Invalid Authorization Header");
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            throw new RuntimeException("Invalid or Expired Token");
        }

        // Forward user information to downstream services
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(
                        "X-User-Id",
                        String.valueOf(jwtUtil.extractUserId(token))
                )
                .header(
                        "X-User-Email",
                        jwtUtil.extractEmail(token)
                )
                .header(
                        "X-Role",
                        jwtUtil.extractRole(token)
                )
                .build();

        return chain.filter(
                exchange.mutate()
                        .request(request)
                        .build()
        );
    }
}