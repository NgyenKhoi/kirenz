package com.example.demo.controller;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dto.internal.ChatMessage;
import com.example.demo.dto.internal.TypingIndicator;
import com.example.demo.dto.request.SendMessageRequest;
import com.example.demo.service.UserPresenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.Instant;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {
    
    private final UserPresenceService presenceService;
    private final RabbitTemplate rabbitTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    
    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        Long senderId = extractUserIdFromPrincipal(principal);
        log.info("📨 Received message from user {} for conversation {} with {} attachments", 
            senderId, request.getConversationId(), 
            request.getAttachments() != null ? request.getAttachments().size() : 0);
        
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            for (int i = 0; i < request.getAttachments().size(); i++) {
                var attachment = request.getAttachments().get(i);
                log.info("  Attachment {}: type={}, url={}", 
                    i + 1, attachment.getType(), attachment.getUrl());
            }
        }
        
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversationId(request.getConversationId());
        chatMessage.setSenderId(senderId);
        chatMessage.setContent(request.getContent());
        chatMessage.setSentAt(Instant.now());
        
        // Convert MediaUploadResponse to MediaAttachment
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            java.util.List<ChatMessage.MediaAttachment> attachments = request.getAttachments().stream()
                .map(uploadResponse -> {
                    ChatMessage.MediaAttachment attachment = new ChatMessage.MediaAttachment();
                    attachment.setType(uploadResponse.getType());
                    attachment.setCloudinaryPublicId(uploadResponse.getCloudinaryPublicId());
                    attachment.setUrl(uploadResponse.getUrl());
                    attachment.setMetadata(uploadResponse.getMetadata());
                    return attachment;
                })
                .collect(java.util.stream.Collectors.toList());
            chatMessage.setAttachments(attachments);
        }
        
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.CHAT_INPUT_ROUTING_KEY,
                chatMessage
            );
            log.info("✅ Successfully published message to RabbitMQ - Exchange: {}, RoutingKey: {}, ConversationId: {}", 
                RabbitMQConfig.CHAT_EXCHANGE, 
                RabbitMQConfig.CHAT_INPUT_ROUTING_KEY,
                chatMessage.getConversationId());
        } catch (Exception e) {
            log.error("❌ Failed to publish message to RabbitMQ: {}", e.getMessage(), e);
            throw e;
        }
    }
    
    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingIndicator indicator, Principal principal) {
        Long userId = extractUserIdFromPrincipal(principal);
        indicator.setUserId(userId);
        
        log.debug("User {} typing in conversation {}: {}", userId, indicator.getConversationId(), indicator.getIsTyping());
        
        messagingTemplate.convertAndSend(
            "/topic/typing." + indicator.getConversationId(),
            indicator
        );
    }
    
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        String sessionId = headerAccessor.getSessionId();
        
        log.info("🔌 WebSocket CONNECT event - SessionId: {}, Principal: {}", sessionId, principal);
        
        if (principal != null && sessionId != null) {
            Long userId = extractUserIdFromPrincipal(principal);
            log.info("✅ WebSocket connected successfully - User: {}, Session: {}", userId, sessionId);
            presenceService.userConnected(userId, sessionId);
        } else {
            log.warn("⚠️ WebSocket connection missing principal or sessionId - Principal: {}, SessionId: {}", principal, sessionId);
        }
    }
    
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = headerAccessor.getUser();
        String sessionId = event.getSessionId();
        
        if (principal != null && sessionId != null) {
            Long userId = extractUserIdFromPrincipal(principal);
            log.info("WebSocket disconnected - User: {}, Session: {}", userId, sessionId);
            presenceService.userDisconnected(userId, sessionId);
        }
    }
    
    private Long extractUserIdFromPrincipal(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("Principal is null");
        }
        
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException e) {
            log.error("Failed to parse user ID from principal: {}", principal.getName());
            throw new IllegalArgumentException("Invalid user ID in principal", e);
        }
    }
}
