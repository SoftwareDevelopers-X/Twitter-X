package com.twitterx.chatservice.config;

import com.twitterx.chatservice.security.UserPrincipal;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Runs on every inbound STOMP frame. On CONNECT specifically, we read the userId
 * that ChatHandshakeInterceptor stashed in the WebSocket session attributes and
 * attach it as the frame's Principal. From then on, every STOMP frame on this
 * session carries that Principal automatically (Spring handles this for us),
 * so @MessageMapping methods can just do Principal.getName() to get the userId.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        System.out.println("=== STOMP Frame: " + accessor.getCommand() + " for destination: " + accessor.getDestination() + " ===");

        Object userIdAttr = accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get(ChatHandshakeInterceptor.USER_ID_ATTRIBUTE)
                : null;

        if (userIdAttr instanceof Long userId) {
            accessor.setUser(new UserPrincipal(userId));
            System.out.println("  UserPrincipal set on STOMP accessor: " + userId + " for command: " + accessor.getCommand());
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        } else {
            System.out.println("  Warning: No valid userId found in session attributes for command: " + accessor.getCommand());
        }

        return message;
    }
}

