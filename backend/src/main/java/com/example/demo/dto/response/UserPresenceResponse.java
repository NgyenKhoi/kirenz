package com.example.demo.dto.response;

import com.example.demo.enums.PresenceStatus;
import lombok.Data;
import java.time.Instant;

@Data
public class UserPresenceResponse {
    private Long userId;
    private String username;
    private PresenceStatus status;
    private Instant lastSeen;
}
