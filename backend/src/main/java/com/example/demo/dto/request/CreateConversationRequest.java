package com.example.demo.dto.request;

import com.example.demo.enums.ConversationType;
import lombok.Data;
import java.util.List;

@Data
public class CreateConversationRequest {
    private ConversationType type;
    private String name;
    private List<Long> participantIds;
}
