package com.example.demo.dto.response;

import com.example.demo.enums.MessageType;
import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class MessageResponse {
    private String id;
    private String conversationId;
    private Long senderId;
    private String senderName;
    private String content;
    private List<MediaAttachmentResponse> attachments;
    private MessageType type;
    private Instant sentAt;
    private List<MessageStatusResponse> statusList;
}
