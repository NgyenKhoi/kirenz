package com.example.demo.repository.mongo;

import com.example.demo.document.Conversation;
import com.example.demo.enums.ConversationType;
import com.example.demo.enums.EntityStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends MongoRepository<Conversation, String> {
    
    List<Conversation> findByParticipantIdsContainingAndStatusOrderByUpdatedAtDesc(Long userId, EntityStatus status);
    
    Optional<Conversation> findByIdAndStatus(String id, EntityStatus status);
    
    @Query("{ 'type': ?0, 'participantIds': { $all: ?1, $size: ?2 }, 'status': ?3 }")
    Optional<Conversation> findByTypeAndParticipantIdsAndStatus(
        ConversationType type, 
        List<Long> participantIds, 
        int size, 
        EntityStatus status
    );
    
    List<Conversation> findByStatusOrderByUpdatedAtDesc(EntityStatus status);
    
    List<Conversation> findByStatusAndDeletedAtBefore(EntityStatus status, Instant deletedAt);
    
    @Query("{ 'participantIds': { $all: ?0 }, 'status': ?1 }")
    List<Conversation> findByAllParticipantsAndStatus(List<Long> participantIds, EntityStatus status);
}
