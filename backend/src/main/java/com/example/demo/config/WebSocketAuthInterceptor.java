package com.example.demo.config;

import com.example.demo.service.JwtService;
import com.example.demo.service.UserPresenceService;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {
    
    private final JwtService jwtService;
    private final UserPresenceService userPresenceService;
    
    public WebSocketAuthInterceptor(JwtService jwtService, @Lazy UserPresenceService userPresenceService) {
        this.jwtService = jwtService;
        this.userPresenceService = userPresenceService;
    }
    
    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null) {
            StompCommand command = accessor.getCommand();
            
            if (StompCommand.CONNECT.equals(command)) {
                log.info("🔐 WebSocket CONNECT intercepted - SessionId: {}", accessor.getSessionId());
                
                String authToken = accessor.getFirstNativeHeader("Authorization");
                log.info("📝 Authorization header: {}", authToken != null ? "Present" : "Missing");
                
                if (authToken != null && authToken.startsWith("Bearer ")) {
                    String token = authToken.substring(7);
                    
                    try {
                        JWTClaimsSet claims = jwtService.validateToken(token);
                        String userId = claims.getSubject();
                        
                        Authentication authentication = new UsernamePasswordAuthenticationToken(
                            userId,
                            null,
                            Collections.emptyList()
                        );
                        
                        accessor.setUser(authentication);
                        log.info("✅ WebSocket authentication successful for user: {}", userId);
                        
                        // Mark user as online
                        userPresenceService.userConnected(Long.parseLong(userId), accessor.getSessionId());
                        
                    } catch (Exception e) {
                        log.error("❌ WebSocket authentication failed: {}", e.getMessage(), e);
                        throw new IllegalArgumentException("Invalid JWT token");
                    }
                } else {
                    log.error("❌ No Authorization header found in WebSocket connection");
                    throw new IllegalArgumentException("Missing JWT token");
                }
            } else if (StompCommand.DISCONNECT.equals(command)) {
                log.info("🔌 WebSocket DISCONNECT intercepted - SessionId: {}", accessor.getSessionId());
                
                // Mark user as offline
                if (accessor.getUser() != null) {
                    String userId = accessor.getUser().getName();
                    userPresenceService.userDisconnected(Long.parseLong(userId), accessor.getSessionId());
                    log.info("✅ User {} marked as offline", userId);
                }
            }
        }
        
        return message;
    }
}
