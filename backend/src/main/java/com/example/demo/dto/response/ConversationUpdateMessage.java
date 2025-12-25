package com.example.demo.dto.response;

import com.example.demo.enums.ConversationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * DTO for conversation list updates sent to user queues.
 * Uses MessageSummary instead of full MessageResponse to minimize payload size
 * and prevent accidental transmission of attachment data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationUpdateMessage {
    private String conversationId;
    private ConversationType conversationType;
    private String conversationName;
    private MessageSummary lastMessage;
    private Integer unreadCount;
    private Instant updatedAt;
    private List<ParticipantResponse> participants;
}