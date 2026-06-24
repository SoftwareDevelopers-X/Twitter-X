package com.twitterx.chatservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${gateway.user-id-header:X-User-Id}")
    private String userIdHeader;

    private final StompAuthChannelInterceptor stompAuthChannelInterceptor;

    public WebSocketConfig(StompAuthChannelInterceptor stompAuthChannelInterceptor) {
        this.stompAuthChannelInterceptor = stompAuthChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Client connects to: ws(s)://<gateway-host>/chat-service/ws  (via gateway route)
        // SockJS fallback included for environments where raw WS is blocked (corporate proxies etc).
        registry.addEndpoint("/ws")
                .addInterceptors(new ChatHandshakeInterceptor(userIdHeader))
                // In production lock this down to your actual frontend origin(s) instead of "*"
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Also expose a raw (non-SockJS) endpoint for native mobile clients / testing tools
        // like Postman or wscat that speak STOMP over plain WebSocket.
        registry.addEndpoint("/ws")
                .addInterceptors(new ChatHandshakeInterceptor(userIdHeader))
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Messages the client SENDS go to destinations prefixed /app -> routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");

        // Messages the SERVER broadcasts go out on /topic (group/broadcast) and /queue (point-to-point)
        // Using a simple in-memory broker here. For multi-instance chat-service behind the gateway,
        // swap this for an external broker (RabbitMQ STOMP plugin) so messages fan out across instances -
        // see the comment in the README section below.
        registry.enableSimpleBroker("/topic", "/queue");

        // Enables sending messages directly to a specific user's session via
        // SimpMessagingTemplate.convertAndSendToUser(userId, "/queue/...", payload)
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(stompAuthChannelInterceptor);
    }
}
