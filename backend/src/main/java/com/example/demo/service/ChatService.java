package com.example.demo.service;

import com.example.demo.config.RabbitMQConfig;
import com.example.demo.document.Conversation;
import com.example.demo.document.Message;
import com.example.demo.dto.internal.ChatMessage;
import com.example.demo.dto.request.CreateConversationRequest;
import com.example.demo.dto.request.SendMessageRequest;
import com.example.demo.dto.response.ConversationResponse;
import com.example.demo.dto.response.MediaAttachmentResponse;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.dto.response.ParticipantResponse;
import com.example.demo.entities.Profile;
import com.example.demo.entities.User;
import com.example.demo.enums.ConversationType;
import com.example.demo.enums.DeliveryStatus;
import com.example.demo.enums.EntityStatus;
import com.example.demo.enums.MessageType;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.ChatMapper;
import com.example.demo.repository.jpa.ProfileRepository;
import com.example.demo.repository.jpa.UserRepository;
import com.example.demo.repository.mongo.ConversationRepository;
import com.example.demo.repository.mongo.MessageRepository;
import com.example.demo.util.MessageSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ChatMapper chatMapper;
    private final RabbitTemplate rabbitTemplate;
    private final RateLimiterService rateLimiterService;
    private final MessageSanitizer messageSanitizer;
    private final SimpMessagingTemplate messagingTemplate;

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_MESSAGE_LENGTH = 10000;

    @Transactional
    public ConversationResponse createConversation(CreateConversationRequest request, Long createdBy) {
        if (request.getParticipantIds() == null || request.getParticipantIds().size() < 2) {
            throw new AppException(ErrorCode.INVALID_PARTICIPANT_LIST);
        }

        if (request.getType() == ConversationType.DIRECT && request.getParticipantIds().size() != 2) {
            throw new AppException(ErrorCode.INVALID_CONVERSATION_TYPE);
        }

        List<User> participants = userRepository.findAllById(request.getParticipantIds());
        if (participants.size() != request.getParticipantIds().size()) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        boolean allActive = participants.stream()
                .allMatch(user -> user.getStatus() == EntityStatus.ACTIVE);
        if (!allActive) {
            throw new AppException(ErrorCode.USER_NOT_FOUND);
        }

        Conversation conversation = chatMapper.toConversation(request);
        conversation.setCreatedBy(createdBy);
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());
        conversation.setStatus(EntityStatus.ACTIVE);

        Conversation savedConversation = conversationRepository.save(conversation);
        ConversationResponse response = enrichConversationResponse(savedConversation, createdBy);

        for (Long participantId : savedConversation.getParticipantIds()) {
            if (!participantId.equals(createdBy)) {
                messagingTemplate.convertAndSendToUser(
                        participantId.toString(),
                        "/queue/conversations",
                        response
                );
            }
        }

        return response;
    }

    public ConversationResponse getConversation(String conversationId, Long userId) {
        Conversation conversation = conversationRepository
                .findByIdAndStatus(conversationId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!conversation.getParticipantIds().contains(userId)) {
            throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
        }

        return enrichConversationResponse(conversation, userId);
    }

    public List<ConversationResponse> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository
                .findByParticipantIdsContainingAndStatusOrderByUpdatedAtDesc(userId, EntityStatus.ACTIVE);

        return conversations.stream()
                .map(conversation -> enrichConversationResponse(conversation, userId))
                .collect(Collectors.toList());
    }

    @Transactional
    public ConversationResponse getOrCreateDirectConversation(Long user1Id, Long user2Id) {
        userRepository.findByIdAndStatus(user1Id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        userRepository.findByIdAndStatus(user2Id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        List<Long> participantIds = List.of(user1Id, user2Id);
        List<Conversation> existingConversations = conversationRepository
                .findByAllParticipantsAndStatus(participantIds, EntityStatus.ACTIVE);

        Conversation existingConversation = existingConversations.stream()
                .filter(conv -> conv.getType() == ConversationType.DIRECT)
                .filter(conv -> conv.getParticipantIds().size() == 2)
                .findFirst()
                .orElse(null);

        if (existingConversation != null) {
            return enrichConversationResponse(existingConversation, user1Id);
        }

        CreateConversationRequest request = new CreateConversationRequest();
        request.setType(ConversationType.DIRECT);
        request.setParticipantIds(participantIds);

        return createConversation(request, user1Id);
    }

    public List<MessageResponse> getMessages(String conversationId, Long userId, int page, int size) {
        Conversation conversation = conversationRepository
                .findByIdAndStatus(conversationId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!conversation.getParticipantIds().contains(userId)) {
            throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
        }

        int pageSize = size > 0 ? size : DEFAULT_PAGE_SIZE;
        Pageable pageable = PageRequest.of(page, pageSize);

        Page<Message> messagePage = messageRepository
                .findByConversationIdAndStatusOrderBySentAtDesc(conversationId, EntityStatus.ACTIVE, pageable);

        List<MessageResponse> messages = messagePage.getContent().stream()
                .map(this::enrichMessageResponse)
                .collect(Collectors.toList());

        java.util.Collections.reverse(messages);
        return messages;
    }

    @Transactional
    public void markAsRead(String conversationId, Long userId) {
        Conversation conversation = conversationRepository
                .findByIdAndStatus(conversationId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!conversation.getParticipantIds().contains(userId)) {
            throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
        }

        List<Message> unreadMessages = messageRepository
                .findUnreadMessagesByConversationAndUser(conversationId, userId, EntityStatus.ACTIVE);

        Instant now = Instant.now();
        for (Message message : unreadMessages) {
            if (message.getStatusList() != null) {
                for (Message.MessageStatus status : message.getStatusList()) {
                    if (status.getUserId().equals(userId) && status.getStatus() != DeliveryStatus.READ) {
                        status.setStatus(DeliveryStatus.READ);
                        status.setTimestamp(now);
                    }
                }
            }
        }

        messageRepository.saveAll(unreadMessages);
    }

    public MessageResponse sendMessage(SendMessageRequest request, Long senderId) {
        rateLimiterService.checkMessageRateLimit(senderId);

        boolean hasContent = request.getContent() != null && !request.getContent().trim().isEmpty();
        boolean hasAttachments = request.getAttachments() != null && !request.getAttachments().isEmpty();

        if (!hasContent && !hasAttachments) {
            throw new AppException(ErrorCode.EMPTY_MESSAGE_CONTENT);
        }

        String sanitizedContent = null;
        if (hasContent) {
            messageSanitizer.validateLength(request.getContent(), MAX_MESSAGE_LENGTH);
            sanitizedContent = messageSanitizer.sanitize(request.getContent());
        }

        Conversation conversation = conversationRepository
                .findByIdAndStatus(request.getConversationId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        if (!conversation.getParticipantIds().contains(senderId)) {
            throw new AppException(ErrorCode.USER_NOT_IN_CONVERSATION);
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversationId(request.getConversationId());
        chatMessage.setSenderId(senderId);
        chatMessage.setContent(sanitizedContent);
        chatMessage.setSentAt(Instant.now());

        MessageType messageType = MessageType.TEXT;
        if (hasAttachments) {
            boolean hasVideo = request.getAttachments().stream()
                    .anyMatch(att -> "VIDEO".equalsIgnoreCase(att.getType()));
            messageType = hasVideo ? MessageType.VIDEO : MessageType.IMAGE;
        }
        chatMessage.setType(messageType);

        if (hasAttachments) {
            List<ChatMessage.MediaAttachment> attachments = request.getAttachments().stream()
                    .map(upload -> {
                        ChatMessage.MediaAttachment att = new ChatMessage.MediaAttachment();
                        att.setType(upload.getType());
                        att.setCloudinaryPublicId(upload.getCloudinaryPublicId());
                        att.setUrl(upload.getUrl());
                        att.setMetadata(upload.getMetadata());
                        return att;
                    })
                    .collect(Collectors.toList());
            chatMessage.setAttachments(attachments);
        }

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.CHAT_INPUT_ROUTING_KEY,
                chatMessage
        );

        MessageResponse response = new MessageResponse();
        response.setConversationId(chatMessage.getConversationId());
        response.setSenderId(chatMessage.getSenderId());
        response.setContent(chatMessage.getContent());
        response.setType(chatMessage.getType());
        response.setSentAt(chatMessage.getSentAt());

        return response;
    }

    @Transactional
    public void processMessage(ChatMessage chatMessage) {
        Conversation conversation = conversationRepository
                .findByIdAndStatus(chatMessage.getConversationId(), EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.CONVERSATION_NOT_FOUND));

        MessageType messageType = MessageType.TEXT;
        if (chatMessage.getAttachments() != null && !chatMessage.getAttachments().isEmpty()) {
            boolean hasVideo = chatMessage.getAttachments().stream()
                    .anyMatch(att -> "VIDEO".equalsIgnoreCase(att.getType()));
            messageType = hasVideo ? MessageType.VIDEO : MessageType.IMAGE;
        }

        Message message = new Message();
        message.setConversationId(chatMessage.getConversationId());
        message.setSenderId(chatMessage.getSenderId());
        message.setContent(chatMessage.getContent());
        message.setType(messageType);
        message.setSentAt(chatMessage.getSentAt());
        message.setStatus(EntityStatus.ACTIVE);

        if (chatMessage.getAttachments() != null) {
            List<Message.MediaAttachment> attachments = chatMessage.getAttachments().stream()
                    .map(att -> {
                        Message.MediaAttachment m = new Message.MediaAttachment();
                        m.setType(att.getType());
                        m.setCloudinaryPublicId(att.getCloudinaryPublicId());
                        m.setUrl(att.getUrl());
                        m.setMetadata(att.getMetadata());
                        return m;
                    })
                    .collect(Collectors.toList());
            message.setAttachments(attachments);
        }

        List<Message.MessageStatus> statusList = new ArrayList<>();
        for (Long participantId : conversation.getParticipantIds()) {
            Message.MessageStatus status = new Message.MessageStatus();
            status.setUserId(participantId);
            status.setStatus(participantId.equals(chatMessage.getSenderId())
                    ? DeliveryStatus.READ
                    : DeliveryStatus.SENT);
            status.setTimestamp(Instant.now());
            statusList.add(status);
        }
        message.setStatusList(statusList);

        Message savedMessage = messageRepository.save(message);

        Conversation.LastMessage lastMessage = new Conversation.LastMessage();
        lastMessage.setMessageId(savedMessage.getId());
        lastMessage.setContent(savedMessage.getContent());
        lastMessage.setSenderId(savedMessage.getSenderId());
        lastMessage.setSentAt(savedMessage.getSentAt());

        conversation.setLastMessage(lastMessage);
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        chatMessage.setMessageId(savedMessage.getId());

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.CHAT_EXCHANGE,
                RabbitMQConfig.CHAT_OUTPUT_ROUTING_KEY,
                chatMessage
        );
    }

    private ConversationResponse enrichConversationResponse(Conversation conversation, Long userId) {
        ConversationResponse response = chatMapper.toConversationResponse(conversation);

        List<ParticipantResponse> participants = new ArrayList<>();
        for (Long participantId : conversation.getParticipantIds()) {
            User user = userRepository.findById(participantId).orElse(null);
            if (user != null) {
                Profile profile = profileRepository.findByUser_Id(participantId).orElse(null);
                ParticipantResponse participantResponse = new ParticipantResponse();
                participantResponse.setUserId(participantId);
                participantResponse.setUsername(user.getEmail());
                participantResponse.setDisplayName(profile != null ? profile.getFullName() : user.getEmail());
                participants.add(participantResponse);
            }
        }
        response.setParticipants(participants);

        if (conversation.getLastMessage() != null) {
            MessageResponse last = new MessageResponse();
            last.setId(conversation.getLastMessage().getMessageId());
            last.setContent(conversation.getLastMessage().getContent());
            last.setSenderId(conversation.getLastMessage().getSenderId());
            last.setSentAt(conversation.getLastMessage().getSentAt());
            last.setConversationId(conversation.getId());
            response.setLastMessage(last);
        }

        List<Message> unreadMessages = messageRepository
                .findUnreadMessagesByConversationAndUser(conversation.getId(), userId, EntityStatus.ACTIVE);
        response.setUnreadCount(unreadMessages.size());

        return response;
    }

    private MessageResponse enrichMessageResponse(Message message) {
        MessageResponse response = chatMapper.toMessageResponse(message);

        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        if (sender != null) {
            Profile profile = profileRepository.findByUser_Id(message.getSenderId()).orElse(null);
            response.setSenderName(profile != null ? profile.getFullName() : sender.getEmail());
        }

        return response;
    }

    public MessageResponse convertToMessageResponse(ChatMessage chatMessage) {
        MessageResponse response = new MessageResponse();
        response.setId(chatMessage.getMessageId());
        response.setConversationId(chatMessage.getConversationId());
        response.setSenderId(chatMessage.getSenderId());
        response.setContent(chatMessage.getContent());
        response.setType(chatMessage.getType());
        response.setSentAt(chatMessage.getSentAt());

        User sender = userRepository.findById(chatMessage.getSenderId()).orElse(null);
        if (sender != null) {
            Profile profile = profileRepository.findByUser_Id(chatMessage.getSenderId()).orElse(null);
            response.setSenderName(profile != null ? profile.getFullName() : sender.getEmail());
        }

        if (chatMessage.getAttachments() != null) {
            List<MediaAttachmentResponse> attachments = chatMessage.getAttachments().stream()
                    .map(att -> {
                        MediaAttachmentResponse r = new MediaAttachmentResponse();
                        r.setType(att.getType());
                        r.setCloudinaryPublicId(att.getCloudinaryPublicId());
                        r.setUrl(att.getUrl());
                        r.setMetadata(att.getMetadata());
                        return r;
                    })
                    .collect(Collectors.toList());
            response.setAttachments(attachments);
        }

        return response;
    }
}
