package com.example.demo.config.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

/**
 * Migration: Create messages collection and indexes
 * Version: 002
 * Date: 2025-12-03
 * Requirements: 7.5
 */
@Slf4j
@ChangeUnit(id = "002-create-messages-collection", order = "002", author = "chat-system")
public class DatabaseChangeLog002_CreateMessagesCollection {

    /**
     * Creates messages collection with indexes
     * Requirements 7.5: Create collection with indexes on conversationId, senderId, and status
     */
    @Execution
    public void createMessagesCollection(MongoDatabase mongoDatabase) {
        log.info("Migration 002: Creating messages collection");
        
        // Create collection
        mongoDatabase.createCollection("messages");
        log.info("Created messages collection");
        
        MongoCollection<Document> collection = mongoDatabase.getCollection("messages");
        
        // Create compound index on conversationId and sentAt
        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("conversationId"),
                Indexes.descending("sentAt")
            ),
            new IndexOptions().name("idx_messages_conversationId_sentAt")
        );
        log.info("Created index: idx_messages_conversationId_sentAt");
        
        // Create compound index on senderId and sentAt
        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("senderId"),
                Indexes.descending("sentAt")
            ),
            new IndexOptions().name("idx_messages_senderId_sentAt")
        );
        log.info("Created index: idx_messages_senderId_sentAt");
        
        // Create index on status
        collection.createIndex(
            Indexes.ascending("status"),
            new IndexOptions().name("idx_messages_status")
        );
        log.info("Created index: idx_messages_status");
    }

    /**
     * Rollback: Drop messages collection
     */
    @RollbackExecution
    public void rollbackMessagesCollection(MongoDatabase mongoDatabase) {
        log.info("Migration 002 Rollback: Dropping messages collection");
        mongoDatabase.getCollection("messages").drop();
        log.info("Dropped messages collection");
    }
}
