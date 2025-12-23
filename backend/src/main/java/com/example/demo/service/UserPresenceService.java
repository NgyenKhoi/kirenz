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
    
    public void userDisconnected(Long userId, String sessionId) {
        log.info("User {} disconnected with session {}", userId, sessionId);
        
        SessionInfo sessionInfo = activeSessions.get(userId);
        if (sessionInfo != null && sessionInfo.getSessionId().equals(sessionId)) {
            activeSessions.remove(userId);
            
            userRepository.findById(userId).ifPresent(user -> {
                user.setLastSeen(Instant.now());
                userRepository.save(user);
            });
            
            broadcastPresenceUpdate(userId, PresenceStatus.OFFLINE);
        }
    }
    
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
                userRepository.findById(userId).ifPresent(user -> {
                    user.setLastSeen(Instant.now());
                    userRepository.save(user);
                });
                
                broadcastPresenceUpdate(userId, PresenceStatus.OFFLINE);
            }
        });
    }
    
    public void updateHeartbeat(Long userId) {
        SessionInfo sessionInfo = activeSessions.get(userId);
        if (sessionInfo != null) {
            sessionInfo.setLastHeartbeat(Instant.now());
        }
    }
    
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
