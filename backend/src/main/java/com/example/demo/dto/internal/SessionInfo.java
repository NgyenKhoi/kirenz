package com.example.demo.dto.internal;

import lombok.Data;
import java.time.Instant;

@Data
public class SessionInfo {
    private String sessionId;
    private Long userId;
    private Instant connectedAt;
    private Instant lastHeartbeat;
}
