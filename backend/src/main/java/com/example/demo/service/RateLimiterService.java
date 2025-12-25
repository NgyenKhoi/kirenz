package com.example.demo.service;

import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

@Service
public class RateLimiterService {
    
    private static final int MAX_MESSAGES_PER_SECOND = 10;
    private static final long TIME_WINDOW_SECONDS = 1;
    
    private final Map<Long, Queue<Instant>> userMessageTimestamps = new ConcurrentHashMap<>();
    private boolean enabled = true;
    

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
            throw new AppException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
        
        timestamps.add(now);
    }
    
    public void clearUserRateLimit(Long userId) {
        userMessageTimestamps.remove(userId);
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
