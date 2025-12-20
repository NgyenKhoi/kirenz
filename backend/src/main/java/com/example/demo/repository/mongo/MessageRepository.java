package com.example.demo.repository.mongo;

import com.example.demo.document.Message;
import com.example.demo.enums.EntityStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends MongoRepository<Message, String> {
    
    Page<Message> findByConversationIdAndStatusOrderBySentAtDesc(
        String conversationId, 
        EntityStatus status, 
        Pageable pageable
    );
    
    List<Message> findByConversationIdAndStatusOrderBySentAtDesc(
        String conversationId, 
        EntityStatus status
    );
    
    Optional<Message> findByIdAndStatus(String id, EntityStatus status);
    
    List<Message> findBySenderIdAndStatusOrderBySentAtDesc(Long senderId, EntityStatus status);
    
    long countByConversationIdAndStatus(String conversationId, EntityStatus status);
    
    @Query("{ 'conversationId': ?0, 'statusList': { $elemMatch: { 'userId': ?1, 'status': { $ne: 'READ' } } }, 'status': ?2 }")
    List<Message> findUnreadMessagesByConversationAndUser(
        String conversationId, 
        Long userId, 
        EntityStatus status
    );
    
    List<Message> findByStatusAndDeletedAtBefore(EntityStatus status, Instant deletedAt);
    
    @Query("{ 'conversationId': ?0, 'sentAt': { $lt: ?1 }, 'status': ?2 }")
    Page<Message> findByConversationIdAndSentAtBeforeAndStatus(
        String conversationId, 
        Instant sentAt, 
        EntityStatus status, 
        Pageable pageable
    );
}
