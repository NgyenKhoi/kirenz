package com.example.demo.mapper;

import com.example.demo.document.Conversation;
import com.example.demo.document.Message;
import com.example.demo.dto.request.CreateConversationRequest;
import com.example.demo.dto.request.SendMessageRequest;
import com.example.demo.dto.response.ConversationResponse;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.dto.response.MessageStatusResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {MediaMapper.class})
public interface ChatMapper {
    
    @Mapping(target = "senderName", ignore = true)
    MessageResponse toMessageResponse(Message message);
    
    List<MessageResponse> toMessageResponseList(List<Message> messages);
    
    @Mapping(target = "participants", ignore = true)
    @Mapping(target = "unreadCount", ignore = true)
    @Mapping(target = "lastMessage", ignore = true)
    ConversationResponse toConversationResponse(Conversation conversation);
    
    List<ConversationResponse> toConversationResponseList(List<Conversation> conversations);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "lastMessage", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Conversation toConversation(CreateConversationRequest request);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "senderId", ignore = true)
    @Mapping(target = "sentAt", ignore = true)
    @Mapping(target = "statusList", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "attachments", ignore = true)
    @Mapping(target = "type", ignore = true)
    Message toMessage(SendMessageRequest request);
    
    MessageStatusResponse toMessageStatusResponse(Message.MessageStatus messageStatus);
    
    List<MessageStatusResponse> toMessageStatusResponseList(List<Message.MessageStatus> messageStatusList);
}
