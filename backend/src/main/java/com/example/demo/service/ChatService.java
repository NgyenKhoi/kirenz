package com.example.demo.service;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.document.Conversation;
import com.example.demo.document.Message;
import com.example.demo.dto.internal.ChatMessage;
import com.example.demo.dto.request.CreateConversationRequest;
import com.example.demo.dto.request.SendMessageRequest;
import com.example.demo.dto.response.ConversationResponse;
import com.example.demo.dto.response.MediaAttachmentResponse;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.dto.response.ParticipantResponse;
import com.example.demo.entities.Profile;
import com.example.demo.entities.User;
import com.example.demo.enums.ConversationType;
import com.example.demo.enums.DeliveryStatus;
import com.example.demo.enums.EntityStatus;
import com.example.demo.enums.MessageType;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.ChatMapper;
import com.example.demo.repository.jpa.ProfileRepository;
import com.example.demo.repository.jpa.UserRepository;
import com.example.demo.repository.mongo.ConversationRepository;
import com.example.demo.repository.mongo.MessageRepository;
import com.example.demo.util.MediaValidator;
import com.example.demo.util.MessageSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {
    
    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ChatMapper chatMapper;
    private final RabbitTemplate rabbitTemplate;
    private final CloudinaryService cloudinaryService;
    private final RateLimiterService rateLimiterService;
    private final MessageSanitizer messageSanitizer;
    private final MediaValidator mediaValidator;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;
    
    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_MESSAGE_LENGTH = 10000;
    
    /**
     * Creates a new conversation with the specified participants.
     * Validates that all participants exist and are active users.
     * For direct conversations, ensures exactly 2 participants.
     * For group conversations, ensures at least 2 participants.
     */
    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request, Long createdBy) {
        log.info("Creating conversation of type {} with {} participants", 
            request.getType(), request.getParticipantIds().size());
        
        // Validate participant list
        if (request.getParticipantIds() == null || request.getParticipantIds().size() < 2) {
            throw new AppException(ErrorCode.INVALID_PARTICIPANT_LIST);
        }
        
        // Validate conversation type
        if (request.getType() == ConversationType.DIRECT && request.getParticipantIds().size() != 2) {
            throw new AppException(ErrorCode.INVALID_CONVERSATION_TYPE);
        }
        
        // Validate all participants exist and are active
        List<User> participants = userRepository.findAllById(request.getParticipantIds());
        if (participants.size() != request.getParticipantIds().size()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        // Check all participants are active
        boolean allActive = participants.stream()
            .allMatch(user -> user.getStatus() == EntityStatus.ACTIVE);
        if (!allActive) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }
        
        // Create conversation
        Conversation conversation = chatMapper.toConversation(request);
        conversation.setCreatedBy(createdBy);
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());
        conversation.setStatus(EntityStatus.ACTIVE);
        
        Conversation savedConversation = conversationRepository.save(conversation);
        
        log.info("Created conversation with ID: {}", savedConversation.getId());
        
        ConversationResponse response = enrichConversationResponse(savedConversation, createdBy);
        
        // Broadcast to all participants via WebSocket
        for (Long participantId : savedConversation.getParticipantIds()) {
            if (!participantId.equals(createdBy)) {
                try {
                    messagingTemplate.convertAndSendToUser(
                        participantId.toString(),
                        "/queue/conversations",
                        response
                    );
                    log.info("Sent conversation notification to user {}", participantId);
                } catch (Exception e) {
                    log.error("Failed to send conversation notification to user {}: {}", participantId, e.getMessage());
                }
            }
        }
        
        return response;
    }
    
    /**
     * Retrieves a conversation by ID with access control.
     * Ensures the requesting user is a participant in the conversation.
     */
    public ConversationResponse getConversation(String conversationId, Long userId) {
        log.info("Fetching conversation {} for user {}", conversationId, userId);
        
        Conversation conversation = conversationRepository
            .findByIdAndStatus(conversationId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
        
        // Verify user is a participant
        if (!conversation.getParticipantIds().contains(userId)) {
            throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
        }
        
        return enrichConversationResponse(conversation, userId);
    }
    
    /**
     * Retrieves all conversations for a specific user.
     * Returns conversations ordered by most recently updated.
     */
    public List<ConversationResponse> getUserConversations(Long userId) {
        log.info("Fetching conversations for user {}", userId);
        
        List<Conversation> conversations = conversationRepository
            .findByParticipantIdsContainingAndStatusOrderByUpdatedAtDesc(userId, EntityStatus.ACTIVE);
        
        return conversations.stream()
            .map(conversation -> enrichConversationResponse(conversation, userId))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets or creates a direct conversation between two users.
     * If a direct conversation already exists between the users, returns it.
     * Otherwise, creates a new direct conversation.
     */
    @Transactional
    public ConversationResponse getOrCreateDirectConversation(Long user1Id, Long user2Id) {
        log.info("Getting or creating direct conversation between users {} and {}", user1Id, user2Id);
        
        // Validate both users exist
        userRepository.findByIdAndStatus(user1Id, EntityStatus.ACTIVE)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        userRepository.findByIdAndStatus(user2Id, EntityStatus.ACTIVE)
            .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Try to find existing direct conversation
        List<Long> participantIds = List.of(user1Id, user2Id);
        List<Conversation> existingConversations = conversationRepository
            .findByAllParticipantsAndStatus(participantIds, EntityStatus.ACTIVE);
        
        // Filter for direct conversations with exactly these 2 participants
        Conversation existingConversation = existingConversations.stream()
            .filter(conv -> conv.getType() == ConversationType.DIRECT)
            .filter(conv -> conv.getParticipantIds().size() == 2)
            .findFirst()
            .orElse(null);
        
        if (existingConversation != null) {
            log.info("Found existing direct conversation: {}", existingConversation.getId());
            return enrichConversationResponse(existingConversation, user1Id);
        }
        
        // Create new direct conversation
        CreateConversationRequest request = new CreateConversationRequest();
        request.setType(ConversationType.DIRECT);
        request.setParticipantIds(participantIds);
        
        return createConversation(request, user1Id);
    }
    
    /**
     * Retrieves messages for a conversation with pagination.
     * Returns messages in descending chronological order (newest first).
     * Default page size is 50 messages.
     */
    public List<MessageResponse> getMessages(String conversationId, Long userId, int page, int size) {
        log.info("Fetching messages for conversation {} (page: {}, size: {})", conversationId, page, size);
        
        // Verify conversation exists and user has access
        Conversation conversation = conversationRepository
            .findByIdAndStatus(conversationId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
        
        if (!conversation.getParticipantIds().contains(userId)) {
            throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
        }
        
        // Fetch messages with pagination
        int pageSize = size > 0 ? size : DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(page, pageSize);
        
        Page<Message> messagePage = messageRepository
            .findByConversationIdAndStatusOrderBySentAtDesc(conversationId, EntityStatus.ACTIVE, pageable);
        
        // Reverse to get oldest first (for chat display)
        List<MessageResponse> messages = messagePage.getContent().stream()
            .map(this::enrichMessageResponse)
            .collect(Collectors.toList());
        
        java.util.Collections.reverse(messages);
        return messages;
    }
    
    /**
     * Marks all messages in a conversation as read for a specific user.
     * Updates the delivery status to READ for all unread messages.
     */
    @Transactional
    public void markAsRead(String conversationId, Long userId) {
        log.info("Marking messages as read for conversation {} and user {}", conversationId, userId);
        
        // Verify conversation exists and user has access
        Conversation conversation = conversationRepository
            .findByIdAndStatus(conversationId, EntityStatus.ACTIVE)
            .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
        
        if (!conversation.getParticipantIds().contains(userId)) {
            throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
        }
        
        // Find all unread messages for this user
        List<Message> unreadMessages = messageRepository
            .findUnreadMessagesByConversationAndUser(conversationId, userId, EntityStatus.ACTIVE);
        
        // Update status to READ
        Instant now = Instant.now();
        for (Message message : unreadMessages) {
            if (message.getStatusList() != null) {
                for (Message.MessageStatus status : message.getStatusList()) {
                    if (status.getUserId().equals(userId) && status.getStatus() != DeliveryStatus.READ) {
                        status.setStatus(DeliveryStatus.READ);
                        status.setTimestamp(now);
                    }
                }
            }
        }
        
        messageRepository.saveAll(unreadMessages);
        
        log.info("Marked {} messages as read", unreadMessages.size());
    }
    
    /**
     * Sends a message by publishing it to RabbitMQ chat.exchange with chat.input routing key.
     * Validates that the content is not empty and the user is a participant in the conversation.
     * Handles media attachments by uploading them to Cloudinary.
     * If media upload fails, saves the message without media attachment.
     * Applies rate limiting and message sanitization for security.
     */
    public MessageResponse sendMessage(SendMessageRequest request, Long senderId) {
        log.info("Sending message from user {} to conversation {}", senderId, request.getConversationId());
        
        // Check rate limit (10 messages per second per user)
        rateLimiterService.checkMessageRateLimit(senderId);
        
        // Validate that either content or attachments exist
        boolean hasContent = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasAttachments = request.getAttachments() != null && !request.getAttachments().isEmpty();
        
        if (!hasContent && !hasAttachments) {
            throw new AppException(ErrorCode.EMPTY_MESSAGE_CONTENT);
        }
        
        // Sanitize message content to prevent XSS (if present)
        String sanitizedContent = null;
        if (hasContent) {
            messageSanitizer.validateLength(request.getContent(), MAX_MESSAGE_LENGTH);
            sanitizedContent = messageSanitizer.sanitize(request.getContent());
        }
        
        // Verify conversation exists and user has access
        Conversation conversation = conversationRepository
            .findByIdAndStatus(request.getConversationId(), EntityStatus.ACTIVE)
            .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
        
        // Verify user is a participant (access control)
        if (!conversation.getParticipantIds().contains(senderId)) {
            throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
        }
        
        // Create ChatMessage for RabbitMQ with sanitized content
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversationId(request.getConversationId());
        chatMessage.setSenderId(senderId);
        chatMessage.setContent(sanitizedContent);
        chatMessage.setSentAt(Instant.now());
        
        // Determine message type based on attachments (already uploaded)
        MessageType messageType = MessageType.TEXT;
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            // Check if any attachment is video
            boolean hasVideo = request.getAttachments().stream()
                .anyMatch(att -> "VIDEO".equalsIgnoreCase(att.getType()));
            messageType = hasVideo ? MessageType.VIDEO : MessageType.IMAGE;
        }
        chatMessage.setType(messageType);
        
        // Convert uploaded attachments to ChatMessage.MediaAttachment
        if (request.getAttachments() != null && !request.getAttachments().isEmpty()) {
            List<ChatMessage.MediaAttachment> attachments = request.getAttachments().stream()
                .map(uploadResponse -> {
                    ChatMessage.MediaAttachment attachment = new ChatMessage.MediaAttachment();
                    attachment.setType(uploadResponse.getType());
                    attachment.setCloudinaryPublicId(uploadResponse.getCloudinaryPublicId());
                    attachment.setUrl(uploadResponse.getUrl());
                    attachment.setMetadata(uploadResponse.getMetadata());
                    return attachment;
                })
                .collect(Collectors.toList());
            chatMessage.setAttachments(attachments);
        }
        
        // Publish to RabbitMQ chat.exchange with chat.input routing key
        try {
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.CHAT_INPUT_ROUTING_KEY,
                chatMessage
            );
            log.info("Published message to RabbitMQ for processing");
        } catch (Exception e) {
            log.error("Failed to publish message to RabbitMQ: {}", e.getMessage());
            throw new AppException(ErrorCode.RABBITMQ_PUBLISH_FAILED);
        }
        
        // Return a response (actual message will be persisted by processMessage)
        MessageResponse response = new MessageResponse();
        response.setConversationId(chatMessage.getConversationId());
        response.setSenderId(chatMessage.getSenderId());
        response.setContent(chatMessage.getContent());
        response.setType(chatMessage.getType());
        response.setSentAt(chatMessage.getSentAt());
        
        return response;
    }
    
    /**
     * Processes a message from RabbitMQ chat.input.queue.
     * Persists the message to MongoDB and updates the conversation's last message.
     * After persisting, publishes the message to chat.exchange with chat.output routing key.
     * This method is called by the RabbitMQ consumer.
     */
    @Transactional
    public void processMessage(ChatMessage chatMessage) {
        log.info("Processing message for conversation {} with {} attachments", 
            chatMessage.getConversationId(),
            chatMessage.getAttachments() != null ? chatMessage.getAttachments().size() : 0);
        
        try {
            // Verify conversation exists
            Conversation conversation = conversationRepository
                .findByIdAndStatus(chatMessage.getConversationId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));
            
            // Determine message type based on attachments
            MessageType messageType = MessageType.TEXT;
            if (chatMessage.getAttachments() != null && !chatMessage.getAttachments().isEmpty()) {
                boolean hasVideo = chatMessage.getAttachments().stream()
                    .anyMatch(att -> "VIDEO".equalsIgnoreCase(att.getType()));
                messageType = hasVideo ? MessageType.VIDEO : MessageType.IMAGE;
            }
            
            // Create Message document
            Message message = new Message();
            message.setConversationId(chatMessage.getConversationId());
            message.setSenderId(chatMessage.getSenderId());
            message.setContent(chatMessage.getContent());
            message.setType(messageType);
            message.setSentAt(chatMessage.getSentAt());
            
            // Convert ChatMessage.MediaAttachment to Message.MediaAttachment
            if (chatMessage.getAttachments() != null && !chatMessage.getAttachments().isEmpty()) {
                List<Message.MediaAttachment> messageAttachments = chatMessage.getAttachments().stream()
                    .map(chatAttachment -> {
                        Message.MediaAttachment msgAttachment = new Message.MediaAttachment();
                        msgAttachment.setType(chatAttachment.getType());
                        msgAttachment.setCloudinaryPublicId(chatAttachment.getCloudinaryPublicId());
                        msgAttachment.setUrl(chatAttachment.getUrl());
                        msgAttachment.setMetadata(chatAttachment.getMetadata());
                        return msgAttachment;
                    })
                    .collect(Collectors.toList());
                message.setAttachments(messageAttachments);
            }
            
            message.setStatus(EntityStatus.ACTIVE);
            
            // Initialize delivery status for all participants
            List<Message.MessageStatus> statusList = new ArrayList<>();
            for (Long participantId : conversation.getParticipantIds()) {
                Message.MessageStatus status = new Message.MessageStatus();
                status.setUserId(participantId);
                
                // Sender gets READ status, others get SENT
                if (participantId.equals(chatMessage.getSenderId())) {
                    status.setStatus(DeliveryStatus.READ);
                } else {
                    status.setStatus(DeliveryStatus.SENT);
                }
                status.setTimestamp(Instant.now());
                
                statusList.add(status);
            }
            message.setStatusList(statusList);
            
            // Save message to MongoDB
            Message savedMessage = messageRepository.save(message);
            log.info("Persisted message with ID: {}", savedMessage.getId());
            
            // Update conversation's last message
            Conversation.LastMessage lastMessage = new Conversation.LastMessage();
            lastMessage.setMessageId(savedMessage.getId());
            lastMessage.setContent(savedMessage.getContent());
            lastMessage.setSenderId(savedMessage.getSenderId());
            lastMessage.setSentAt(savedMessage.getSentAt());
            
            conversation.setLastMessage(lastMessage);
            conversation.setUpdatedAt(Instant.now());
            conversationRepository.save(conversation);
            
            // Update ChatMessage with the persisted message ID
            chatMessage.setMessageId(savedMessage.getId());
            
            // Publish to chat.exchange with chat.output routing key for delivery
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.CHAT_OUTPUT_ROUTING_KEY,
                chatMessage
            );
            log.info("Published message to chat.output for delivery");
            
        } catch (AppException e) {
            log.error("Application error processing message: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error processing message: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.RABBITMQ_CONSUME_FAILED);
        }
    }
    
    /**
     * Enriches a conversation response with participant details and unread count.
     */
    private ConversationResponse enrichConversationResponse(Conversation conversation, Long userId) {
        ConversationResponse response = chatMapper.toConversationResponse(conversation);
        
        // Add participant details
        List<ParticipantResponse> participants = new ArrayList<>();
        for (Long participantId : conversation.getParticipantIds()) {
            User user = userRepository.findById(participantId).orElse(null);
            if (user != null) {
                Profile profile = profileRepository.findByUser_Id(participantId).orElse(null);
                
                ParticipantResponse participant = new ParticipantResponse();
                participant.setUserId(participantId);
                participant.setUsername(user.getEmail());
                participant.setDisplayName(profile != null ? profile.getFullName() : user.getEmail());
                
                participants.add(participant);
            }
        }
        response.setParticipants(participants);
        
        // Add last message if exists
        if (conversation.getLastMessage() != null) {
            MessageResponse lastMessage = new MessageResponse();
            lastMessage.setId(conversation.getLastMessage().getMessageId());
            lastMessage.setContent(conversation.getLastMessage().getContent());
            lastMessage.setSenderId(conversation.getLastMessage().getSenderId());
            lastMessage.setSentAt(conversation.getLastMessage().getSentAt());
            lastMessage.setConversationId(conversation.getId());
            response.setLastMessage(lastMessage);
        }
        
        // Calculate unread count
        List<Message> unreadMessages = messageRepository
            .findUnreadMessagesByConversationAndUser(conversation.getId(), userId, EntityStatus.ACTIVE);
        response.setUnreadCount(unreadMessages.size());
        
        return response;
    }
    
    /**
     * Enriches a message response with sender name.
     */
    private MessageResponse enrichMessageResponse(Message message) {
        MessageResponse response = chatMapper.toMessageResponse(message);
        
        // Add sender name
        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        if (sender != null) {
            Profile profile = profileRepository.findByUser_Id(message.getSenderId()).orElse(null);
            response.setSenderName(profile != null ? profile.getFullName() : sender.getEmail());
        }
        
        return response;
    }
    
    /**
     * Converts ChatMessage to MessageResponse for WebSocket broadcasting.
     * This method is public so it can be called by the RabbitMQ consumer.
     */
    public MessageResponse convertToMessageResponse(ChatMessage chatMessage) {
        MessageResponse response = new MessageResponse();
        response.setId(chatMessage.getMessageId());
        response.setConversationId(chatMessage.getConversationId());
        response.setSenderId(chatMessage.getSenderId());
        response.setContent(chatMessage.getContent());
        response.setType(chatMessage.getType());
        response.setSentAt(chatMessage.getSentAt());
        
        // Add sender name
        User sender = userRepository.findById(chatMessage.getSenderId()).orElse(null);
        if (sender != null) {
            Profile profile = profileRepository.findByUser_Id(chatMessage.getSenderId()).orElse(null);
            response.setSenderName(profile != null ? profile.getFullName() : sender.getEmail());
        }
        
        // Convert attachments if present
        if (chatMessage.getAttachments() != null && !chatMessage.getAttachments().isEmpty()) {
            List<MediaAttachmentResponse> attachmentResponses = chatMessage.getAttachments().stream()
                .map(attachment -> {
                    MediaAttachmentResponse attachmentResponse = new MediaAttachmentResponse();
                    attachmentResponse.setType(attachment.getType());
                    attachmentResponse.setCloudinaryPublicId(attachment.getCloudinaryPublicId());
                    attachmentResponse.setUrl(attachment.getUrl());
                    attachmentResponse.setMetadata(attachment.getMetadata());
                    return attachmentResponse;
                })
                .collect(Collectors.toList());
            response.setAttachments(attachmentResponses);
        }
        
        return response;
    }
}
