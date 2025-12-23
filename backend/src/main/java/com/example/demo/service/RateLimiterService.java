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
    

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        log.info("Rate limiting {}", enabled ? "enabled" : "disabled");
    }
    

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
        
        timestamps.removeIf(timestamp -> timestamp.isBefore(windowStart));
        
        if (timestamps.size() >= MAX_MESSAGES_PER_SECOND) {
            log.warn("Rate limit exceeded for user {}: {} messages in last {} second(s)", 
                userId, timestamps.size(), TIME_WINDOW_SECONDS);
            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        
        timestamps.add(now);
        
        log.debug("User {} message count in window: {}/{}", 
            userId, timestamps.size(), MAX_MESSAGES_PER_SECOND);
    }
    
    public void clearUserRateLimit(Long userId) {
        userMessageTimestamps.remove(userId);
        log.info("Cleared rate limit data for user {}", userId);
    }
    
    public int getCurrentMessageCount(Long userId) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(TIME_WINDOW_SECONDS);
        
        Queue<Instant> timestamps = userMessageTimestamps.get(userId);
        if (timestamps == null) {
            return 0;
        }
        
        timestamps.removeIf(timestamp -> timestamp.isBefore(windowStart));
        
        return timestamps.size();
    }
}
