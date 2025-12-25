package com.example.demo.dto.response;

import com.example.demo.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSummary {
    private String id;
    private String conversationId;
    private Long senderId;
    private String senderName;
    private MessageType type;
    private String previewText;
    private Instant sentAt;
}