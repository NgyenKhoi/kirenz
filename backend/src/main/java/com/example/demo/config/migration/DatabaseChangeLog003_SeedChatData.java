package com.example.demo.config.migration;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Migration: Seed sample chat data
 * Version: 003
 * Date: 2025-12-03
 * Purpose: Add sample conversations and messages for testing
 */
@Slf4j
@ChangeUnit(id = "003-seed-chat-data", order = "003", author = "chat-system")
public class DatabaseChangeLog003_SeedChatData {

    /**
     * Seeds sample conversations and messages
     */
    @Execution
    public void seedChatData(MongoDatabase mongoDatabase) {
        log.info("Migration 003: Seeding sample chat data");
        
        seedConversations(mongoDatabase);
        seedMessages(mongoDatabase);
        
        log.info("Migration 003: Completed seeding chat data");
    }

    private void seedConversations(MongoDatabase mongoDatabase) {
        MongoCollection<Document> conversations = mongoDatabase.getCollection("conversations");
        
        // Check if data already exists
        if (conversations.countDocuments() > 0) {
            log.info("Conversations already exist, skipping seed");
            return;
        }
        
        Instant now = Instant.now();
        
        // Sample conversation 1: Direct message between user 1 and user 2
        Document conv1 = new Document()
            .append("_id", "conv_001")
            .append("type", "DIRECT")
            .append("name", null)
            .append("participantIds", Arrays.asList(1L, 2L))
            .append("createdBy", 1L)
            .append("createdAt", now)
            .append("updatedAt", now)
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        // Sample conversation 2: Direct message between user 1 and user 3
        Document conv2 = new Document()
            .append("_id", "conv_002")
            .append("type", "DIRECT")
            .append("name", null)
            .append("participantIds", Arrays.asList(1L, 3L))
            .append("createdBy", 1L)
            .append("createdAt", now)
            .append("updatedAt", now)
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        // Sample conversation 3: Group chat
        Document conv3 = new Document()
            .append("_id", "conv_003")
            .append("type", "GROUP")
            .append("name", "Team Discussion")
            .append("participantIds", Arrays.asList(1L, 2L, 3L))
            .append("createdBy", 1L)
            .append("createdAt", now)
            .append("updatedAt", now)
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        // Sample conversation 4: Another group chat
        Document conv4 = new Document()
            .append("_id", "conv_004")
            .append("type", "GROUP")
            .append("name", "Project Planning")
            .append("participantIds", Arrays.asList(1L, 2L))
            .append("createdBy", 2L)
            .append("createdAt", now)
            .append("updatedAt", now)
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        List<Document> conversationsList = Arrays.asList(conv1, conv2, conv3, conv4);
        conversations.insertMany(conversationsList);
        
        log.info("Seeded {} conversations", conversationsList.size());
    }

    private void seedMessages(MongoDatabase mongoDatabase) {
        MongoCollection<Document> messages = mongoDatabase.getCollection("messages");
        
        // Check if data already exists
        if (messages.countDocuments() > 0) {
            log.info("Messages already exist, skipping seed");
            return;
        }
        
        Instant now = Instant.now();
        Instant oneHourAgo = now.minusSeconds(3600);
        Instant twoHoursAgo = now.minusSeconds(7200);
        Instant threeDaysAgo = now.minusSeconds(259200);
        
        // Messages for conversation 1 (Direct: User 1 <-> User 2)
        Document msg1 = new Document()
            .append("_id", new ObjectId())
            .append("conversationId", "conv_001")
            .append("senderId", 1L)
            .append("content", "Hey! How are you doing?")
            .append("attachments", Arrays.asList())
            .append("type", "TEXT")
            .append("sentAt", threeDaysAgo)
            .append("statusList", Arrays.asList(
                new Document().append("userId", 1L).append("status", "READ").append("timestamp", threeDaysAgo),
                new Document().append("userId", 2L).append("status", "READ").append("timestamp", threeDaysAgo.plusSeconds(300))
            ))
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        Document msg2 = new Document()
            .append("_id", new ObjectId())
            .append("conversationId", "conv_001")
            .append("senderId", 2L)
            .append("content", "I'm good! Thanks for asking. How about you?")
            .append("attachments", Arrays.asList())
            .append("type", "TEXT")
            .append("sentAt", threeDaysAgo.plusSeconds(600))
            .append("statusList", Arrays.asList(
                new Document().append("userId", 1L).append("status", "READ").append("timestamp", threeDaysAgo.plusSeconds(900)),
                new Document().append("userId", 2L).append("status", "READ").append("timestamp", threeDaysAgo.plusSeconds(600))
            ))
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        Document msg3 = new Document()
            .append("_id", new ObjectId())
            .append("conversationId", "conv_001")
            .append("senderId", 1L)
            .append("content", "Doing great! Want to grab coffee later?")
            .append("attachments", Arrays.asList())
            .append("type", "TEXT")
            .append("sentAt", twoHoursAgo)
            .append("statusList", Arrays.asList(
                new Document().append("userId", 1L).append("status", "READ").append("timestamp", twoHoursAgo),
                new Document().append("userId", 2L).append("status", "DELIVERED").append("timestamp", twoHoursAgo.plusSeconds(10))
            ))
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        // Messages for conversation 2 (Direct: User 1 <-> User 3)
        Document msg4 = new Document()
            .append("_id", new ObjectId())
            .append("conversationId", "conv_002")
            .append("senderId", 1L)
            .append("content", "Did you finish the report?")
            .append("attachments", Arrays.asList())
            .append("type", "TEXT")
            .append("sentAt", oneHourAgo)
            .append("statusList", Arrays.asList(
                new Document().append("userId", 1L).append("status", "READ").append("timestamp", oneHourAgo),
                new Document().append("userId", 3L).append("status", "READ").append("timestamp", oneHourAgo.plusSeconds(120))
            ))
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        Document msg5 = new Document()
            .append("_id", new ObjectId())
            .append("conversationId", "conv_002")
            .append("senderId", 3L)
            .append("content", "Yes! Just sent it to your email.")
            .append("attachments", Arrays.asList())
            .append("type", "TEXT")
            .append("sentAt", oneHourAgo.plusSeconds(300))
            .append("statusList", Arrays.asList(
                new Document().append("userId", 1L).append("status", "DELIVERED").append("timestamp", oneHourAgo.plusSeconds(305)),
                new Document().append("userId", 3L).append("status", "READ").append("timestamp", oneHourAgo.plusSeconds(300))
            ))
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        // Messages for conversation 3 (Group: Team Discussion)
        Document msg6 = new Document()
            .append("_id", new ObjectId())
            .append("conversationId", "conv_003")
            .append("senderId", 1L)
            .append("content", "Welcome to the team discussion group!")
            .append("attachments", Arrays.asList())
            .append("type", "SYSTEM")
            .append("sentAt", threeDaysAgo)
            .append("statusList", Arrays.asList(
                new Document().append("userId", 1L).append("status", "READ").append("timestamp", threeDaysAgo),
                new Document().append("userId", 2L).append("status", "READ").append("timestamp", threeDaysAgo.plusSeconds(60)),
                new Document().append("userId", 3L).append("status", "READ").append("timestamp", threeDaysAgo.plusSeconds(120))
            ))
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        Document msg7 = new Document()
            .append("_id", new ObjectId())
            .append("conversationId", "conv_003")
            .append("senderId", 2L)
            .append("content", "Thanks! Excited to be here.")
            .append("attachments", Arrays.asList())
            .append("type", "TEXT")
            .append("sentAt", threeDaysAgo.plusSeconds(180))
            .append("statusList", Arrays.asList(
                new Document().append("userId", 1L).append("status", "READ").append("timestamp", threeDaysAgo.plusSeconds(240)),
                new Document().append("userId", 2L).append("status", "READ").append("timestamp", threeDaysAgo.plusSeconds(180)),
                new Document().append("userId", 3L).append("status", "READ").append("timestamp", threeDaysAgo.plusSeconds(300))
            ))
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        Document msg8 = new Document()
            .append("_id", new ObjectId())
            .append("conversationId", "conv_003")
            .append("senderId", 3L)
            .append("content", "Let's schedule our first meeting for next week.")
            .append("attachments", Arrays.asList())
            .append("type", "TEXT")
            .append("sentAt", now.minusSeconds(1800))
            .append("statusList", Arrays.asList(
                new Document().append("userId", 1L).append("status", "DELIVERED").append("timestamp", now.minusSeconds(1795)),
                new Document().append("userId", 2L).append("status", "DELIVERED").append("timestamp", now.minusSeconds(1790)),
                new Document().append("userId", 3L).append("status", "READ").append("timestamp", now.minusSeconds(1800))
            ))
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        // Messages for conversation 4 (Group: Project Planning)
        Document msg9 = new Document()
            .append("_id", new ObjectId())
            .append("conversationId", "conv_004")
            .append("senderId", 2L)
            .append("content", "Let's discuss the project timeline.")
            .append("attachments", Arrays.asList())
            .append("type", "TEXT")
            .append("sentAt", now.minusSeconds(900))
            .append("statusList", Arrays.asList(
                new Document().append("userId", 1L).append("status", "SENT").append("timestamp", now.minusSeconds(900)),
                new Document().append("userId", 2L).append("status", "READ").append("timestamp", now.minusSeconds(900))
            ))
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        Document msg10 = new Document()
            .append("_id", new ObjectId())
            .append("conversationId", "conv_004")
            .append("senderId", 1L)
            .append("content", "Sure! I think we can finish by end of month.")
            .append("attachments", Arrays.asList())
            .append("type", "TEXT")
            .append("sentAt", now.minusSeconds(600))
            .append("statusList", Arrays.asList(
                new Document().append("userId", 1L).append("status", "READ").append("timestamp", now.minusSeconds(600)),
                new Document().append("userId", 2L).append("status", "SENT").append("timestamp", now.minusSeconds(595))
            ))
            .append("status", "ACTIVE")
            .append("deletedAt", null);
        
        List<Document> messagesList = Arrays.asList(
            msg1, msg2, msg3, msg4, msg5, msg6, msg7, msg8, msg9, msg10
        );
        messages.insertMany(messagesList);
        
        log.info("Seeded {} messages", messagesList.size());
    }

    /**
     * Rollback: Remove seeded data
     */
    @RollbackExecution
    public void rollbackSeedData(MongoDatabase mongoDatabase) {
        log.info("Migration 003 Rollback: Removing seeded chat data");
        
        MongoCollection<Document> conversations = mongoDatabase.getCollection("conversations");
        MongoCollection<Document> messages = mongoDatabase.getCollection("messages");
        
        // Delete seeded conversations
        conversations.deleteMany(new Document("_id", new Document("$in", 
            Arrays.asList("conv_001", "conv_002", "conv_003", "conv_004"))));
        
        // Delete all messages for seeded conversations
        messages.deleteMany(new Document("conversationId", new Document("$in", 
            Arrays.asList("conv_001", "conv_002", "conv_003", "conv_004"))));
        
        log.info("Removed seeded chat data");
    }
}
