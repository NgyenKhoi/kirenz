package com.example.demo.dto.internal;

import lombok.Data;

@Data
public class TypingIndicator {
    private String conversationId;
    private Long userId;
    private String username;
    private Boolean isTyping;
}
