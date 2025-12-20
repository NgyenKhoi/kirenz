package com.example.demo.service;

import com.example.demo.document.Conversation;
import com.example.demo.dto.internal.SessionInfo;
import com.example.demo.dto.response.UserPresenceResponse;
import com.example.demo.entities.User;
import com.example.demo.enums.EntityStatus;
import com.example.demo.enums.PresenceStatus;
import com.example.demo.repository.jpa.UserRepository;
import com.example.demo.repository.mongo.ConversationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserPresenceService {
    
    private final ConcurrentHashMap<Long, SessionInfo> activeSessions = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    
    private static final long HEARTBEAT_TIMEOUT_MS = 60000; // 60 seconds
    
    /**
     * Marks a user as online and stores their session information.
     * Broadcasts presence update to all users who have conversations with this user.
     */
    public void userConnected(Long userId, String sessionId) {
        log.info("User {} connected with session {}", userId, sessionId);
        
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setUserId(userId);
        sessionInfo.setSessionId(sessionId);
        sessionInfo.setConnectedAt(Instant.now());
        sessionInfo.setLastHeartbeat(Instant.now());
        
        activeSessions.put(userId, sessionInfo);
        
        broadcastPresenceUpdate(userId, PresenceStatus.ONLINE);
    }
    
    /**
     * Marks a user as offline and removes their session information.
     * Broadcasts presence update to all users who have conversations with this user.
     */
    public void userDisconnected(Long userId, String sessionId) {
        log.info("User {} disconnected with session {}", userId, sessionId);
        
        SessionInfo sessionInfo = activeSessions.get(userId);
        if (sessionInfo != null && sessionInfo.getSessionId().equals(sessionId)) {
            activeSessions.remove(userId);
            
            // Update lastSeen in database
            userRepository.findById(userId).ifPresent(user -> {
                user.setLastSeen(Instant.now());
                userRepository.save(user);
            });
            
            broadcastPresenceUpdate(userId, PresenceStatus.OFFLINE);
        }
    }
    
    /**
     * Retrieves the list of online users who are participants in the specified conversation.
     */
    public List<UserPresenceResponse> getOnlineUsers(String conversationId) {
        log.debug("Getting online users for conversation {}", conversationId);
        
        Conversation conversation = conversationRepository
            .findByIdAndStatus(conversationId, EntityStatus.ACTIVE)
            .orElse(null);
        
        if (conversation == null) {
            log.warn("Conversation {} not found or inactive", conversationId);
            return new ArrayList<>();
        }
        
        List<Long> participantIds = conversation.getParticipantIds();
        List<User> users = userRepository.findAllById(participantIds);
        
        return users.stream()
            .map(user -> {
                UserPresenceResponse response = new UserPresenceResponse();
                response.setUserId(user.getId());
                response.setUsername(user.getEmail());
                
                SessionInfo sessionInfo = activeSessions.get(user.getId());
                if (sessionInfo != null) {
                    response.setStatus(PresenceStatus.ONLINE);
                    response.setLastSeen(sessionInfo.getLastHeartbeat());
                } else {
                    response.setStatus(PresenceStatus.OFFLINE);
                    // Use lastSeen from database for offline users
                    response.setLastSeen(user.getLastSeen());
                }
                
                return response;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Broadcasts presence status update to all users who have conversations with the specified user.
     * Sends the update to user-specific queue destinations.
     */
    private void broadcastPresenceUpdate(Long userId, PresenceStatus status) {
        
        List<Conversation> conversations = conversationRepository
            .findByParticipantIdsContainingAndStatusOrderByUpdatedAtDesc(userId, EntityStatus.ACTIVE);
        
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("User {} not found, skipping presence broadcast", userId);
            return;
        }
        
        UserPresenceResponse presenceResponse = new UserPresenceResponse();
        presenceResponse.setUserId(userId);
        presenceResponse.setUsername(user.getEmail());
        presenceResponse.setStatus(status);
        
        SessionInfo sessionInfo = activeSessions.get(userId);
        presenceResponse.setLastSeen(sessionInfo != null ? sessionInfo.getLastHeartbeat() : Instant.now());
        
        conversations.stream()
            .flatMap(conv -> conv.getParticipantIds().stream())
            .filter(participantId -> !participantId.equals(userId))
            .distinct()
            .forEach(participantId -> {
                try {
                    messagingTemplate.convertAndSendToUser(
                        participantId.toString(),
                        "/queue/presence",
                        presenceResponse
                    );
                } catch (Exception e) {
                    log.error("Failed to send presence update to user {}: {}", participantId, e.getMessage());
                }
            });
    }
    
    /**
     * Scheduled task that runs every 30 seconds to check for stale sessions.
     * Disconnects users whose last heartbeat was more than 60 seconds ago.
     */
    @Scheduled(fixedRate = 30000)
    public void checkHeartbeats() {
        
        Instant now = Instant.now();
        List<Long> staleUsers = new ArrayList<>();
        
        activeSessions.forEach((userId, sessionInfo) -> {
            long timeSinceLastHeartbeat = now.toEpochMilli() - sessionInfo.getLastHeartbeat().toEpochMilli();
            
            if (timeSinceLastHeartbeat > HEARTBEAT_TIMEOUT_MS) {
                staleUsers.add(userId);
            }
        });
        
        staleUsers.forEach(userId -> {
            SessionInfo sessionInfo = activeSessions.remove(userId);
            if (sessionInfo != null) {
                // Update lastSeen in database
                userRepository.findById(userId).ifPresent(user -> {
                    user.setLastSeen(Instant.now());
                    userRepository.save(user);
                });
                
                broadcastPresenceUpdate(userId, PresenceStatus.OFFLINE);
            }
        });
    }
    
    /**
     * Updates the last heartbeat timestamp for a user's session.
     * This should be called periodically to keep the session alive.
     */
    public void updateHeartbeat(Long userId) {
        SessionInfo sessionInfo = activeSessions.get(userId);
        if (sessionInfo != null) {
            sessionInfo.setLastHeartbeat(Instant.now());
        }
    }
    
    /**
     * Retrieves presence information for all users in the system.
     */
    public List<UserPresenceResponse> getAllUserPresence() {
        log.debug("Getting presence for all users");
        
        List<User> users = userRepository.findAll();
        
        return users.stream()
            .map(user -> {
                UserPresenceResponse response = new UserPresenceResponse();
                response.setUserId(user.getId());
                response.setUsername(user.getEmail());
                
                SessionInfo sessionInfo = activeSessions.get(user.getId());
                if (sessionInfo != null) {
                    response.setStatus(PresenceStatus.ONLINE);
                    response.setLastSeen(sessionInfo.getLastHeartbeat());
                } else {
                    response.setStatus(PresenceStatus.OFFLINE);
                    // Use lastSeen from database for offline users
                    response.setLastSeen(user.getLastSeen());
                }
                
                return response;
            })
            .collect(Collectors.toList());
    }
}
