package com.twitterx.chatservice.security;

import com.twitterx.chatservice.exception.MissingUserHeaderException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Reads the X-User-Id header that Spring Cloud Gateway injects after validating the JWT,
 * and puts it into the Spring Security context as the authenticated principal.
 *
 * IMPORTANT: this filter trusts the header completely. It must be impossible for this
 * service to be reached except through the gateway (network policy / internal-only routing),
 * otherwise anyone could spoof X-User-Id and impersonate any user.
 */
@Component
public class GatewayUserHeaderFilter extends OncePerRequestFilter {

    @Value("${gateway.user-id-header:X-User-Id}")
    private String userIdHeader;

    @Value("${gateway.user-id-header-required:true}")
    private boolean headerRequired;

    // Actuator/health checks and the WS handshake path are handled separately
    private static final String[] EXCLUDED_PATHS = {"/actuator", "/ws"};

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        boolean excluded = false;
        for (String prefix : EXCLUDED_PATHS) {
            if (path.startsWith(prefix)) {
                excluded = true;
                break;
            }
        }

        if (!excluded) {
            String userIdValue = request.getHeader(userIdHeader);
            if (userIdValue == null || userIdValue.isBlank()) {
                if (headerRequired) {
                    throw new MissingUserHeaderException();
                }
            } else {
                Long userId = Long.parseLong(userIdValue);
                UserPrincipal principal = new UserPrincipal(userId);
                var auth = new UsernamePasswordAuthenticationToken(principal, null, java.util.Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
