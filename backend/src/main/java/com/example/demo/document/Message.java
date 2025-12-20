package com.example.demo.document;

import com.example.demo.enums.DeliveryStatus;
import com.example.demo.enums.EntityStatus;
import com.example.demo.enums.MessageType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "conversation_sentAt_idx", def = "{'conversationId': 1, 'sentAt': -1}"),
    @CompoundIndex(name = "sender_sentAt_idx", def = "{'senderId': 1, 'sentAt': -1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {
    
    @Id
    private String id;
    
    private String conversationId;
    private Long senderId;
    private String content;
    private List<MediaAttachment> attachments;
    private MessageType type;
    private Instant sentAt;
    private List<MessageStatus> statusList;
    
    @Indexed
    private EntityStatus status;
    
    private Instant deletedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MediaAttachment {
        private String type;
        private String cloudinaryPublicId;
        private String url;
        private Map<String, Object> metadata;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageStatus {
        private Long userId;
        private DeliveryStatus status;
        private Instant timestamp;
    }
}
