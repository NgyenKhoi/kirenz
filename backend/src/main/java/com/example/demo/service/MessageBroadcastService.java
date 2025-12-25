package com.example.demo.service;

import com.example.demo.document.Conversation;
import com.example.demo.dto.internal.ChatMessage;
import com.example.demo.dto.response.ConversationUpdateMessage;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.dto.response.MessageSummary;
import com.example.demo.dto.response.ParticipantResponse;
import com.example.demo.entities.Profile;
import com.example.demo.entities.User;
import com.example.demo.enums.EntityStatus;
import com.example.demo.enums.MessageType;
import com.example.demo.repository.jpa.ProfileRepository;
import com.example.demo.repository.jpa.UserRepository;
import com.example.demo.repository.mongo.ConversationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageBroadcastService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ChatService chatService;

    public void broadcastMessage(ChatMessage chatMessage) {
        try {
            log.info("Broadcasting message for conversation {} to dual destinations", chatMessage.getConversationId());
            
            // 1. Broadcast to conversation topic (for active chat window) - FULL DATA
            broadcastToConversationTopic(chatMessage);
            
            // 2. Broadcast to user queues (for conversation list updates) - SUMMARY DATA ONLY
            broadcastToUserQueues(chatMessage);
            
            log.info("Successfully completed dual broadcast for conversation {}", chatMessage.getConversationId());
            
        } catch (Exception e) {
            log.error("Failed to broadcast message for conversation {}: {}", 
                chatMessage.getConversationId(), e.getMessage(), e);
            // Don't rethrow - partial delivery is better than no delivery
        }
    }

    private void broadcastToConversationTopic(ChatMessage chatMessage) {
        try {
            String destination = "/topic/conversation/" + chatMessage.getConversationId();
            
            // Transform to full MessageResponse with all attachments and details
            MessageResponse fullMessage = chatService.convertToMessageResponse(chatMessage);
            
            messagingTemplate.convertAndSend(destination, fullMessage);
            log.debug("Broadcasted full message details to conversation topic: {}", destination);
            
        } catch (Exception e) {
            log.error("Failed to broadcast to conversation topic for conversation {}: {}", 
                chatMessage.getConversationId(), e.getMessage());
            // Continue with other broadcasts
        }
    }
    
    private void broadcastToUserQueues(ChatMessage chatMessage) {
        try {
            log.debug("Starting participant lookup for conversation: {}", chatMessage.getConversationId());
            
            // Get conversation participants
            Conversation conversation = lookupConversationParticipants(chatMessage.getConversationId());
            
            if (conversation == null) {
                log.warn("Conversation not found or inactive for message broadcast: {}", chatMessage.getConversationId());
                return;
            }
            
            List<Long> participantIds = conversation.getParticipantIds();
            log.info("Found {} participants for conversation {}: {}", 
                participantIds.size(), chatMessage.getConversationId(), participantIds);
            
            // Create conversation update with MessageSummary (NO attachment data)
            ConversationUpdateMessage updateMessage = createConversationUpdate(chatMessage, conversation);
            
            // Broadcast to each participant's user queue
            int successfulBroadcasts = 0;
            for (Long participantId : participantIds) {
                try {
                    String userDestination = "/user/" + participantId + "/queue/messages";
                    messagingTemplate.convertAndSend(userDestination, updateMessage);
                    log.debug("Broadcasted conversation update to user queue: {}", userDestination);
                    successfulBroadcasts++;
                    
                } catch (Exception e) {
                    log.error("Failed to broadcast to user queue for participant {}: {}", participantId, e.getMessage());
                }
            }
            
            log.info("Completed user queue broadcasting for conversation {}: {}/{} successful", 
                chatMessage.getConversationId(), successfulBroadcasts, participantIds.size());
            
        } catch (Exception e) {
            log.error("Failed to broadcast to user queues for conversation {}: {}", 
                chatMessage.getConversationId(), e.getMessage());
        }
    }
    
    private ConversationUpdateMessage createConversationUpdate(ChatMessage chatMessage, Conversation conversation) {
        try {
            log.debug("Creating conversation update for conversation {} with message type {}", 
                chatMessage.getConversationId(), chatMessage.getType());
            
            // Transform ChatMessage to MessageSummary (NO attachment data)
            MessageSummary lastMessageSummary = transformToMessageSummary(chatMessage);
            
            List<ParticipantResponse> participants = getParticipantDetails(conversation.getParticipantIds());
            
            // Calculate unread count (simplified - will be 1 for new message)
            Integer unreadCount = 1;
            
            ConversationUpdateMessage updateMessage = ConversationUpdateMessage.builder()
                    .conversationId(chatMessage.getConversationId())
                    .conversationType(conversation.getType())
                    .conversationName(conversation.getName())
                    .lastMessage(lastMessageSummary)
                    .unreadCount(unreadCount)
                    .updatedAt(chatMessage.getSentAt())
                    .participants(participants)
                    .build();
            
            log.debug("Successfully created conversation update with {} participants", 
                participants.size());
            
            return updateMessage;
                    
        } catch (Exception e) {
            log.error("Failed to create conversation update for conversation {}: {}", 
                chatMessage.getConversationId(), e.getMessage());
            
            // Return minimal update on error
            return ConversationUpdateMessage.builder()
                    .conversationId(chatMessage.getConversationId())
                    .conversationType(conversation.getType())
                    .conversationName(conversation.getName())
                    .unreadCount(1)
                    .updatedAt(chatMessage.getSentAt())
                    .build();
        }
    }
    
    private MessageSummary transformToMessageSummary(ChatMessage chatMessage) {
        log.debug("Transforming ChatMessage to MessageSummary for message type: {}", 
            chatMessage.getType());
        
        // Get sender name
        String senderName = getSenderName(chatMessage.getSenderId());
        
        // Generate preview text based on message type and content
        String previewText = generatePreviewText(chatMessage);
        
        MessageSummary summary = MessageSummary.builder()
                .id(chatMessage.getMessageId())
                .conversationId(chatMessage.getConversationId())
                .senderId(chatMessage.getSenderId())
                .senderName(senderName)
                .type(chatMessage.getType())
                .previewText(previewText)
                .sentAt(chatMessage.getSentAt())
                .build();
        
        log.debug("Transformed message to summary format with preview: '{}'", previewText);
        
        return summary;
    }
    
    private String generatePreviewText(ChatMessage chatMessage) {
        // Handle media-only messages first
        if (chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
            return getMediaTypeIndicator(chatMessage.getType());
        }
        
        // Handle messages with both content and attachments
        String content = chatMessage.getContent().trim();
        boolean hasAttachments = chatMessage.getAttachments() != null && !chatMessage.getAttachments().isEmpty();
        
        if (hasAttachments) {
            String mediaIndicator = getMediaTypeIndicator(chatMessage.getType());
            return mediaIndicator + " " + truncateText(content, 80);
        }
        
        // Text-only messages
        return truncateText(content, 100);
    }
    
    private String getMediaTypeIndicator(MessageType messageType) {
        switch (messageType) {
            case IMAGE:
                return "Image";
            case VIDEO:
                return "Video";
            case TEXT:
            default:
                return "";
        }
    }
    
    private String truncateText(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
    
    private String getSenderName(Long senderId) {
        try {
            User sender = userRepository.findById(senderId).orElse(null);
            if (sender != null) {
                Profile profile = profileRepository.findByUser_Id(senderId).orElse(null);
                return profile != null ? profile.getFullName() : sender.getEmail();
            }
        } catch (Exception e) {
            log.error("Failed to get sender name for user {}: {}", senderId, e.getMessage());
        }
        return "Unknown User";
    }
    
    private List<ParticipantResponse> getParticipantDetails(List<Long> participantIds) {
        List<ParticipantResponse> participants = new ArrayList<>();
        
        for (Long participantId : participantIds) {
            try {
                User user = userRepository.findById(participantId).orElse(null);
                if (user != null) {
                    Profile profile = profileRepository.findByUser_Id(participantId).orElse(null);
                    
                    ParticipantResponse participant = new ParticipantResponse();
                    participant.setUserId(participantId);
                    participant.setUsername(user.getEmail());
                    participant.setDisplayName(profile != null ? profile.getFullName() : user.getEmail());
                    
                    participants.add(participant);
                }
            } catch (Exception e) {
                log.error("Failed to get participant details for user {}: {}", participantId, e.getMessage());
            }
        }
        
        return participants;
    }
    
    private Conversation lookupConversationParticipants(String conversationId) {
        try {
            log.debug("Querying conversation participants for conversation: {}", conversationId);
            
            Optional<Conversation> conversationOpt = conversationRepository
                    .findByIdAndStatus(conversationId, EntityStatus.ACTIVE);
            
            if (conversationOpt.isEmpty()) {
                log.warn("Conversation {} not found or not active during participant lookup", conversationId);
                return null;
            }
            
            Conversation conversation = conversationOpt.get();
            List<Long> participantIds = conversation.getParticipantIds();
            
            if (participantIds == null || participantIds.isEmpty()) {
                log.warn("Conversation {} has no participants", conversationId);
                return null;
            }
            
            log.debug("Successfully retrieved {} participants for conversation {}", 
                participantIds.size(), conversationId);
            
            return conversation;
            
        } catch (Exception e) {
            log.error("Error during participant lookup for conversation {}: {}", conversationId, e.getMessage(), e);
            return null;
        }
    }
}