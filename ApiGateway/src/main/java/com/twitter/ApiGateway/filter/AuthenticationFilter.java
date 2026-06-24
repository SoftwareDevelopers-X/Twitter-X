package com.twitter.ApiGateway.filter;

import com.twitter.ApiGateway.config.RouteValidator;
import com.twitter.ApiGateway.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AuthenticationFilter implements GlobalFilter, Ordered {

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

        // Get Authorization header or fallback to 'token' query param
        String authHeader = exchange.getRequest()
                .getHeaders()
                .getFirst(HttpHeaders.AUTHORIZATION);

        String token = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
        } else {
            token = exchange.getRequest()
                    .getQueryParams()
                    .getFirst("token");
        }

        if (token == null || !jwtUtil.validateToken(token)) {
            exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
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

    @Override
    public int getOrder() {
        return -1; // Must run before WebsocketRoutingFilter (2147483646)
    }
}