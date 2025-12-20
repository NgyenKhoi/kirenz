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
 * Migration: Create chat collections and indexes
 * Version: 001
 * Date: 2025-12-03
 * Requirements: 7.4, 7.5
 */
@Slf4j
@ChangeUnit(id = "001-create-chat-collections", order = "001", author = "chat-system")
public class DatabaseChangeLog001_CreateChatCollections {

    /**
     * Creates conversations collection with indexes
     * Requirements 7.4: Create collection with indexes on participantIds and status/updatedAt
     */
    @Execution
    public void createConversationsCollection(MongoDatabase mongoDatabase) {
        log.info("Migration 001: Creating conversations collection");
        
        // Create collection
        mongoDatabase.createCollection("conversations");
        log.info("Created conversations collection");
        
        MongoCollection<Document> collection = mongoDatabase.getCollection("conversations");
        
        // Create index on participantIds
        collection.createIndex(
            Indexes.ascending("participantIds"),
            new IndexOptions().name("idx_conversations_participantIds")
        );
        log.info("Created index: idx_conversations_participantIds");
        
        // Create compound index on status and updatedAt
        collection.createIndex(
            Indexes.compoundIndex(
                Indexes.ascending("status"),
                Indexes.descending("updatedAt")
            ),
            new IndexOptions().name("idx_conversations_status_updatedAt")
        );
        log.info("Created index: idx_conversations_status_updatedAt");
    }

    /**
     * Rollback: Drop conversations collection
     */
    @RollbackExecution
    public void rollbackConversationsCollection(MongoDatabase mongoDatabase) {
        log.info("Migration 001 Rollback: Dropping conversations collection");
        mongoDatabase.getCollection("conversations").drop();
        log.info("Dropped conversations collection");
    }
}
