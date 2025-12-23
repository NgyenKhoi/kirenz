package com.example.demo.integration;

import com.example.demo.document.Conversation;
import com.example.demo.document.Message;
import com.example.demo.dto.request.CreateConversationRequest;
import com.example.demo.dto.request.SendMessageRequest;
import com.example.demo.dto.response.ConversationResponse;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.dto.response.UserPresenceResponse;
import com.example.demo.entities.Profile;
import com.example.demo.entities.User;
import com.example.demo.enums.ConversationType;
import com.example.demo.enums.DeliveryStatus;
import com.example.demo.enums.EntityStatus;
import com.example.demo.enums.MessageType;
import com.example.demo.enums.PresenceStatus;
import com.example.demo.repository.jpa.ProfileRepository;
import com.example.demo.repository.jpa.UserRepository;
import com.example.demo.repository.mongo.ConversationRepository;
import com.example.demo.repository.mongo.MessageRepository;
import com.example.demo.service.ChatService;
import com.example.demo.service.RateLimiterService;
import com.example.demo.service.UserPresenceService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Testcontainers
class ChatIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgresContainer = new PostgreSQLContainer<>("postgres:17")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    @SuppressWarnings("resource")
    static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0")
            .withExposedPorts(27017);

    @Container
    @SuppressWarnings("resource")
    static final RabbitMQContainer rabbitMQContainer = new RabbitMQContainer("rabbitmq:4-management")
            .withExposedPorts(5672);

    @AfterAll
    static void tearDown() {
        if (postgresContainer != null && postgresContainer.isRunning()) {
            postgresContainer.stop();
        }
        if (mongoDBContainer != null && mongoDBContainer.isRunning()) {
            mongoDBContainer.stop();
        }
        if (rabbitMQContainer != null && rabbitMQContainer.isRunning()) {
            rabbitMQContainer.stop();
        }
    }

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgresContainer::getUsername);
        registry.add("spring.datasource.password", postgresContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("mongock.enabled", () -> "false");
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
        registry.add("spring.rabbitmq.host", rabbitMQContainer::getHost);
        registry.add("spring.rabbitmq.port", rabbitMQContainer::getAmqpPort);
        registry.add("spring.rabbitmq.username", () -> "guest");
        registry.add("spring.rabbitmq.password", () -> "guest");
        // Disable rate limiting for tests
        registry.add("chat.rate-limit.enabled", () -> "false");
    }

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserPresenceService userPresenceService;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    private User user1;
    private User user2;
    private User user3;

    @BeforeEach
    void setUp() {
        // Clean up databases
        conversationRepository.deleteAll();
        messageRepository.deleteAll();
        profileRepository.deleteAll();
        userRepository.deleteAll();
        
        // Disable rate limiting for tests
        rateLimiterService.setEnabled(false);
        
        // Create test users
        user1 = createTestUser("user1@test.com", "User One");
        user2 = createTestUser("user2@test.com", "User Two");
        user3 = createTestUser("user3@test.com", "User Three");
    }

    @Test
    void testEndToEndMessageFlow() throws InterruptedException {
        // Create a direct conversation
        CreateConversationRequest conversationRequest = new CreateConversationRequest();
        conversationRequest.setType(ConversationType.DIRECT);
        conversationRequest.setParticipantIds(List.of(user1.getId(), user2.getId()));
        
        ConversationResponse conversation = chatService.createConversation(conversationRequest, user1.getId());
        assertThat(conversation).isNotNull();
        assertThat(conversation.getId()).isNotNull();
        assertThat(conversation.getType()).isEqualTo(ConversationType.DIRECT);
        assertThat(conversation.getParticipants()).hasSize(2);

        // Send a message
        SendMessageRequest messageRequest = new SendMessageRequest();
        messageRequest.setConversationId(conversation.getId());
        messageRequest.setContent("Hello, this is a test message!");
        
        MessageResponse sentMessage = chatService.sendMessage(messageRequest, user1.getId());
        assertThat(sentMessage).isNotNull();
        assertThat(sentMessage.getContent()).isEqualTo("Hello, this is a test message!");

        // Wait for message to be processed through RabbitMQ and persisted to MongoDB
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> messages = messageRepository.findByConversationIdAndStatusOrderBySentAtDesc(
                conversation.getId(), EntityStatus.ACTIVE);
            assertThat(messages).hasSize(1);
            
            Message persistedMessage = messages.get(0);
            assertThat(persistedMessage.getContent()).isEqualTo("Hello, this is a test message!");
            assertThat(persistedMessage.getSenderId()).isEqualTo(user1.getId());
            assertThat(persistedMessage.getConversationId()).isEqualTo(conversation.getId());
            assertThat(persistedMessage.getType()).isEqualTo(MessageType.TEXT);
            assertThat(persistedMessage.getStatusList()).hasSize(2);
            
            // Verify sender has READ status
            Message.MessageStatus senderStatus = persistedMessage.getStatusList().stream()
                .filter(s -> s.getUserId().equals(user1.getId()))
                .findFirst()
                .orElse(null);
            assertThat(senderStatus).isNotNull();
            assertThat(senderStatus.getStatus()).isEqualTo(DeliveryStatus.READ);
            
            // Verify recipient has SENT status
            Message.MessageStatus recipientStatus = persistedMessage.getStatusList().stream()
                .filter(s -> s.getUserId().equals(user2.getId()))
                .findFirst()
                .orElse(null);
            assertThat(recipientStatus).isNotNull();
            assertThat(recipientStatus.getStatus()).isEqualTo(DeliveryStatus.SENT);
        });

        // Verify conversation was updated with last message
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Conversation updatedConversation = conversationRepository.findById(conversation.getId()).orElse(null);
            assertThat(updatedConversation).isNotNull();
            assertThat(updatedConversation.getLastMessage()).isNotNull();
            assertThat(updatedConversation.getLastMessage().getContent()).isEqualTo("Hello, this is a test message!");
        });
    }

    @Test
    void testConversationCreationAndParticipantManagement() {
        // Test direct conversation creation
        CreateConversationRequest directRequest = new CreateConversationRequest();
        directRequest.setType(ConversationType.DIRECT);
        directRequest.setParticipantIds(List.of(user1.getId(), user2.getId()));
        
        ConversationResponse directConversation = chatService.createConversation(directRequest, user1.getId());
        assertThat(directConversation).isNotNull();
        assertThat(directConversation.getType()).isEqualTo(ConversationType.DIRECT);
        assertThat(directConversation.getParticipants()).hasSize(2);
        assertThat(directConversation.getParticipants())
            .extracting("userId")
            .containsExactlyInAnyOrder(user1.getId(), user2.getId());

        // Test group conversation creation
        CreateConversationRequest groupRequest = new CreateConversationRequest();
        groupRequest.setType(ConversationType.GROUP);
        groupRequest.setName("Test Group");
        groupRequest.setParticipantIds(List.of(user1.getId(), user2.getId(), user3.getId()));
        
        ConversationResponse groupConversation = chatService.createConversation(groupRequest, user1.getId());
        assertThat(groupConversation).isNotNull();
        assertThat(groupConversation.getType()).isEqualTo(ConversationType.GROUP);
        assertThat(groupConversation.getName()).isEqualTo("Test Group");
        assertThat(groupConversation.getParticipants()).hasSize(3);
        assertThat(groupConversation.getParticipants())
            .extracting("userId")
            .containsExactlyInAnyOrder(user1.getId(), user2.getId(), user3.getId());

        // Test get user conversations
        List<ConversationResponse> user1Conversations = chatService.getUserConversations(user1.getId());
        assertThat(user1Conversations).hasSize(2);
        
        List<ConversationResponse> user3Conversations = chatService.getUserConversations(user3.getId());
        assertThat(user3Conversations).hasSize(1);
        assertThat(user3Conversations.get(0).getType()).isEqualTo(ConversationType.GROUP);
    }

    @Test
    void testPaginationWithLargeMessageSets() throws InterruptedException {
        // Create a conversation
        CreateConversationRequest conversationRequest = new CreateConversationRequest();
        conversationRequest.setType(ConversationType.DIRECT);
        conversationRequest.setParticipantIds(List.of(user1.getId(), user2.getId()));
        
        ConversationResponse conversation = chatService.createConversation(conversationRequest, user1.getId());

        // Send 100 messages
        int totalMessages = 100;
        for (int i = 0; i < totalMessages; i++) {
            SendMessageRequest messageRequest = new SendMessageRequest();
            messageRequest.setConversationId(conversation.getId());
            messageRequest.setContent("Test message " + i);
            
            chatService.sendMessage(messageRequest, user1.getId());
        }

        // Wait for all messages to be processed
        await().atMost(30, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> allMessages = messageRepository.findByConversationIdAndStatusOrderBySentAtDesc(
                conversation.getId(), EntityStatus.ACTIVE);
            assertThat(allMessages).hasSize(totalMessages);
        });

        // Test pagination - first page (50 messages)
        List<MessageResponse> page1 = chatService.getMessages(conversation.getId(), user1.getId(), 0, 50);
        assertThat(page1).hasSize(50);
        
        // Test pagination - second page (50 messages)
        List<MessageResponse> page2 = chatService.getMessages(conversation.getId(), user1.getId(), 1, 50);
        assertThat(page2).hasSize(50);
        
        // Verify no overlap between pages
        List<String> page1Ids = page1.stream().map(MessageResponse::getId).toList();
        List<String> page2Ids = page2.stream().map(MessageResponse::getId).toList();
        assertThat(page1Ids).doesNotContainAnyElementsOf(page2Ids);
        
        // Test smaller page size
        List<MessageResponse> page1Small = chatService.getMessages(conversation.getId(), user1.getId(), 0, 10);
        assertThat(page1Small).hasSize(10);
        
        List<MessageResponse> page2Small = chatService.getMessages(conversation.getId(), user1.getId(), 1, 10);
        assertThat(page2Small).hasSize(10);
    }

    @Test
    void testPresenceTrackingAcrossConnectionsAndDisconnections() {
        // Create a conversation to test presence
        CreateConversationRequest conversationRequest = new CreateConversationRequest();
        conversationRequest.setType(ConversationType.DIRECT);
        conversationRequest.setParticipantIds(List.of(user1.getId(), user2.getId()));
        ConversationResponse conversation = chatService.createConversation(conversationRequest, user1.getId());

        String sessionId1 = "session-1";
        String sessionId2 = "session-2";

        // Test user connection
        userPresenceService.userConnected(user1.getId(), sessionId1);
        
        // Verify user is marked as online
        List<UserPresenceResponse> onlineUsers = userPresenceService.getOnlineUsers(conversation.getId());
        UserPresenceResponse user1Presence = onlineUsers.stream()
            .filter(u -> u.getUserId().equals(user1.getId()))
            .findFirst()
            .orElse(null);
        assertThat(user1Presence).isNotNull();
        assertThat(user1Presence.getStatus()).isEqualTo(PresenceStatus.ONLINE);

        // Test user disconnection
        userPresenceService.userDisconnected(user1.getId(), sessionId1);
        
        // Verify user is marked as offline
        List<UserPresenceResponse> afterDisconnect = userPresenceService.getOnlineUsers(conversation.getId());
        UserPresenceResponse user1AfterDisconnect = afterDisconnect.stream()
            .filter(u -> u.getUserId().equals(user1.getId()))
            .findFirst()
            .orElse(null);
        assertThat(user1AfterDisconnect).isNotNull();
        assertThat(user1AfterDisconnect.getStatus()).isEqualTo(PresenceStatus.OFFLINE);

        // Test multiple users
        userPresenceService.userConnected(user1.getId(), sessionId1);
        userPresenceService.userConnected(user2.getId(), sessionId2);
        
        List<UserPresenceResponse> multipleOnline = userPresenceService.getOnlineUsers(conversation.getId());
        long onlineCount = multipleOnline.stream()
            .filter(u -> u.getStatus() == PresenceStatus.ONLINE)
            .count();
        assertThat(onlineCount).isEqualTo(2);

        // Disconnect both
        userPresenceService.userDisconnected(user1.getId(), sessionId1);
        userPresenceService.userDisconnected(user2.getId(), sessionId2);
        
        List<UserPresenceResponse> allOffline = userPresenceService.getOnlineUsers(conversation.getId());
        long offlineCount = allOffline.stream()
            .filter(u -> u.getStatus() == PresenceStatus.OFFLINE)
            .count();
        assertThat(offlineCount).isEqualTo(2);
    }

    @Test
    void testMarkMessagesAsRead() throws InterruptedException {
        // Create conversation
        CreateConversationRequest conversationRequest = new CreateConversationRequest();
        conversationRequest.setType(ConversationType.DIRECT);
        conversationRequest.setParticipantIds(List.of(user1.getId(), user2.getId()));
        
        ConversationResponse conversation = chatService.createConversation(conversationRequest, user1.getId());

        // Send messages from user1
        for (int i = 0; i < 5; i++) {
            SendMessageRequest messageRequest = new SendMessageRequest();
            messageRequest.setConversationId(conversation.getId());
            messageRequest.setContent("Message " + i);
            chatService.sendMessage(messageRequest, user1.getId());
        }

        // Wait for messages to be processed
        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> messages = messageRepository.findByConversationIdAndStatusOrderBySentAtDesc(
                conversation.getId(), EntityStatus.ACTIVE);
            assertThat(messages).hasSize(5);
        });

        // Verify user2 has unread messages
        List<Message> unreadMessages = messageRepository.findUnreadMessagesByConversationAndUser(
            conversation.getId(), user2.getId(), EntityStatus.ACTIVE);
        assertThat(unreadMessages).hasSize(5);

        // Mark as read
        chatService.markAsRead(conversation.getId(), user2.getId());

        // Verify messages are now read
        List<Message> stillUnread = messageRepository.findUnreadMessagesByConversationAndUser(
            conversation.getId(), user2.getId(), EntityStatus.ACTIVE);
        assertThat(stillUnread).isEmpty();

        // Verify status was updated to READ
        List<Message> allMessages = messageRepository.findByConversationIdAndStatusOrderBySentAtDesc(
            conversation.getId(), EntityStatus.ACTIVE);
        for (Message message : allMessages) {
            Message.MessageStatus user2Status = message.getStatusList().stream()
                .filter(s -> s.getUserId().equals(user2.getId()))
                .findFirst()
                .orElse(null);
            assertThat(user2Status).isNotNull();
            assertThat(user2Status.getStatus()).isEqualTo(DeliveryStatus.READ);
        }
    }

    @Test
    void testGroupConversationMessageDelivery() throws InterruptedException {
        // Create group conversation
        CreateConversationRequest groupRequest = new CreateConversationRequest();
        groupRequest.setType(ConversationType.GROUP);
        groupRequest.setName("Test Group");
        groupRequest.setParticipantIds(List.of(user1.getId(), user2.getId(), user3.getId()));
        
        ConversationResponse groupConversation = chatService.createConversation(groupRequest, user1.getId());

        // Send message in group
        SendMessageRequest messageRequest = new SendMessageRequest();
        messageRequest.setConversationId(groupConversation.getId());
        messageRequest.setContent("Hello group!");
        
        chatService.sendMessage(messageRequest, user1.getId());

        // Wait for message to be processed
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            List<Message> messages = messageRepository.findByConversationIdAndStatusOrderBySentAtDesc(
                groupConversation.getId(), EntityStatus.ACTIVE);
            assertThat(messages).hasSize(1);
            
            Message message = messages.get(0);
            assertThat(message.getStatusList()).hasSize(3);
            
            // Verify all participants have status
            assertThat(message.getStatusList())
                .extracting("userId")
                .containsExactlyInAnyOrder(user1.getId(), user2.getId(), user3.getId());
            
            // Verify sender has READ, others have SENT
            message.getStatusList().forEach(status -> {
                if (status.getUserId().equals(user1.getId())) {
                    assertThat(status.getStatus()).isEqualTo(DeliveryStatus.READ);
                } else {
                    assertThat(status.getStatus()).isEqualTo(DeliveryStatus.SENT);
                }
            });
        });
    }

    @Test
    void testGetOrCreateDirectConversation() {
        // First call should create a new conversation
        ConversationResponse conversation1 = chatService.getOrCreateDirectConversation(user1.getId(), user2.getId());
        assertThat(conversation1).isNotNull();
        assertThat(conversation1.getType()).isEqualTo(ConversationType.DIRECT);
        assertThat(conversation1.getParticipants()).hasSize(2);

        // Second call should return the same conversation
        ConversationResponse conversation2 = chatService.getOrCreateDirectConversation(user1.getId(), user2.getId());
        assertThat(conversation2.getId()).isEqualTo(conversation1.getId());

        // Reverse order should also return the same conversation
        ConversationResponse conversation3 = chatService.getOrCreateDirectConversation(user2.getId(), user1.getId());
        assertThat(conversation3.getId()).isEqualTo(conversation1.getId());

        // Different users should create a new conversation
        ConversationResponse conversation4 = chatService.getOrCreateDirectConversation(user1.getId(), user3.getId());
        assertThat(conversation4.getId()).isNotEqualTo(conversation1.getId());
    }

    private User createTestUser(String email, String fullName) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("$2a$10$dummyHashForTesting");
        user.setStatus(EntityStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        user = userRepository.save(user);

        Profile profile = new Profile();
        profile.setUser(user);
        profile.setFullName(fullName);
        profile.setUpdatedAt(Instant.now());
        profileRepository.save(profile);

        return user;
    }
}
