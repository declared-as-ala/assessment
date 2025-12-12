package com.chess.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Map;

/**
 * Interceptor to pass user information from WebSocket session to message headers.
 */
@Component
@Slf4j
public class WebSocketChannelInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // On CONNECT, set the user principal from session attributes
            Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes != null && sessionAttributes.containsKey("username")) {
                String username = (String) sessionAttributes.get("username");
                Long userId = (Long) sessionAttributes.get("userId");
                
                // Set the user principal for Spring WebSocket user destinations
                Principal principal = () -> username;
                accessor.setUser(principal);
                
                log.info("WebSocket user authenticated: {} (ID: {})", username, userId);
            }
        }
        
        return message;
    }
}

