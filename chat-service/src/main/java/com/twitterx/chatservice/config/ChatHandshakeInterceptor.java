package com.twitterx.chatservice.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * Runs during the initial HTTP handshake (before it's upgraded to a WebSocket).
 * The gateway has already validated the JWT for this request and attached the
 * X-User-Id header - same as it does for plain REST calls. We pull it out here
 * and stash it in the WebSocket session attributes so the STOMP layer can turn
 * it into a Principal later (see StompAuthChannelInterceptor).
 *
 * If the header is missing, we reject the handshake outright (return false).
 */
public class ChatHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTRIBUTE = "userId";

    private final String userIdHeader;

    public ChatHandshakeInterceptor(String userIdHeader) {
        this.userIdHeader = userIdHeader;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                    WebSocketHandler wsHandler, Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpRequest = servletRequest.getServletRequest();
            
            // Debug: print all headers to see what is passed from the gateway
            System.out.println("=== Websocket Handshake Headers ===");
            java.util.Enumeration<String> headerNames = httpRequest.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    System.out.println("  " + name + ": " + httpRequest.getHeader(name));
                }
            }
            System.out.println("===================================");

            String userIdValue = httpRequest.getHeader(userIdHeader);

            if (userIdValue == null || userIdValue.isBlank()) {
                System.out.println("Handshake rejected: " + userIdHeader + " header is missing or blank!");
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false; // reject handshake - no identity, no connection
            }

            try {
                Long userId = Long.parseLong(userIdValue);
                attributes.put(USER_ID_ATTRIBUTE, userId);
                return true;
            } catch (NumberFormatException e) {
                response.setStatusCode(HttpStatus.BAD_REQUEST);
                return false;
            }
        }

        return false;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
