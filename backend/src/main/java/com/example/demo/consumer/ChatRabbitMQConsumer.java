package com.example.demo.consumer;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.dto.internal.ChatMessage;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatRabbitMQConsumer {
    
    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    
    @RabbitListener(queues = RabbitMQConfig.CHAT_INPUT_QUEUE)
    public void consumeInputMessage(ChatMessage message) {
        try {
            log.info("Consuming message from chat.input.queue for conversation: {}", message.getConversationId());
            chatService.processMessage(message);
            log.info("Successfully processed message for conversation: {}", message.getConversationId());
        } catch (Exception e) {
            log.error("Error processing message from chat.input.queue: {}", e.getMessage(), e);
            throw new AmqpRejectAndDontRequeueException("Failed to process message", e);
        }
    }
    
    @RabbitListener(queues = RabbitMQConfig.CHAT_OUTPUT_QUEUE)
    public void consumeOutputMessage(ChatMessage message) {
        try {
            log.info("Consuming message from chat.output.queue for conversation: {}", message.getConversationId());
            
            // Convert ChatMessage to MessageResponse for frontend
            MessageResponse messageResponse = chatService.convertToMessageResponse(message);
            
            messagingTemplate.convertAndSend(
                "/topic/conversation/" + message.getConversationId(),
                messageResponse
            );
            
            log.info("Successfully broadcasted message to WebSocket subscribers for conversation: {}", 
                message.getConversationId());
        } catch (Exception e) {
            log.error("Error broadcasting message from chat.output.queue: {}", e.getMessage(), e);
            throw new AmqpRejectAndDontRequeueException("Failed to broadcast message", e);
        }
    }
}
