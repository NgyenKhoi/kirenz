package com.example.demo.document;

import com.example.demo.enums.ConversationType;
import com.example.demo.enums.EntityStatus;
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

@Document(collection = "conversations")
@CompoundIndexes({
    @CompoundIndex(name = "status_updatedAt_idx", def = "{'status': 1, 'updatedAt': -1}"),
    @CompoundIndex(name = "type_participants_idx", def = "{'type': 1, 'participantIds': 1}")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    
    @Id
    private String id;
    
    private ConversationType type;
    private String name;
    
    @Indexed
    private List<Long> participantIds;
    
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private LastMessage lastMessage;
    private EntityStatus status;
    private Instant deletedAt;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastMessage {
        private String messageId;
        private String content;
        private Long senderId;
        private Instant sentAt;
    }
}
