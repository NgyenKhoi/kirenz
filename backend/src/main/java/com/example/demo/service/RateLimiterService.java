package com.example.demo.service;

import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
@Slf4j
public class RateLimiterService {
    
    private static final int MAX_MESSAGES_PER_SECOND = 10;
    private static final long TIME_WINDOW_SECONDS = 1;
    
    private final Map<Long, Queue<Instant>> userMessageTimestamps = new ConcurrentHashMap<>();
    private boolean enabled = true;
    
    /**
     * Sets whether rate limiting is enabled.
     * Useful for testing purposes.
     * 
     * @param enabled true to enable rate limiting, false to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("Rate limiting {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Checks if a user has exceeded the rate limit for message sending.
     * Rate limit: 10 messages per second per user.
     * 
     * @param userId The user ID to check
     * @throws AppException if rate limit is exceeded
     */
    public void checkMessageRateLimit(Long userId) {
        if (!enabled) {
            return;
        }
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(TIME_WINDOW_SECONDS);
        
        Queue<Instant> timestamps = userMessageTimestamps.computeIfAbsent(
            userId, 
            k -> new ConcurrentLinkedQueue<>()
        );
        
        // Remove timestamps outside the time window
        timestamps.removeIf(timestamp -> timestamp.isBefore(windowStart));
        
        // Check if user has exceeded rate limit
        if (timestamps.size() >= MAX_MESSAGES_PER_SECOND) {
            log.warn("Rate limit exceeded for user {}: {} messages in last {} second(s)", 
                userId, timestamps.size(), TIME_WINDOW_SECONDS);
            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        
        // Add current timestamp
        timestamps.add(now);
        
        log.debug("User {} message count in window: {}/{}", 
            userId, timestamps.size(), MAX_MESSAGES_PER_SECOND);
    }
    
    /**
     * Clears rate limit data for a user.
     * Useful for testing or administrative purposes.
     * 
     * @param userId The user ID to clear
     */
    public void clearUserRateLimit(Long userId) {
        userMessageTimestamps.remove(userId);
        log.info("Cleared rate limit data for user {}", userId);
    }
    
    /**
     * Gets the current message count for a user within the time window.
     * 
     * @param userId The user ID to check
     * @return The number of messages sent in the current time window
     */
    public int getCurrentMessageCount(Long userId) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(TIME_WINDOW_SECONDS);
        
        Queue<Instant> timestamps = userMessageTimestamps.get(userId);
        if (timestamps == null) {
            return 0;
        }
        
        // Remove old timestamps
        timestamps.removeIf(timestamp -> timestamp.isBefore(windowStart));
        
        return timestamps.size();
    }
}
