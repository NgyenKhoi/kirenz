package com.example.demo.dto.internal;

import com.example.demo.enums.MessageType;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class ChatMessage {
    private String id;
    private String messageId;
    private String conversationId;
    private Long senderId;
    private String content;
    private List<MediaAttachment> attachments;
    private MessageType type;
    private Instant sentAt;
    
    @Data
    public static class MediaAttachment {
        private String type;
        private String cloudinaryPublicId;
        private String url;
        private Map<String, Object> metadata;
    }
}
