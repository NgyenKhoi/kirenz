package com.example.demo.dto.response;

import com.example.demo.enums.ConversationType;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class ConversationResponse {
    private String id;
    private ConversationType type;
    private String name;
    private List<ParticipantResponse> participants;
    private MessageResponse lastMessage;
    private Integer unreadCount;
    private Instant createdAt;
    private Instant updatedAt;
}
