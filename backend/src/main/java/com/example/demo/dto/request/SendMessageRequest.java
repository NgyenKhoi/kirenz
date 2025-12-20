package com.example.demo.dto.request;

import com.example.demo.dto.response.MediaUploadResponse;
import lombok.Data;
import java.util.List;

@Data
public class SendMessageRequest {
    private String conversationId;
    private String content;
    private List<MediaUploadResponse> attachments;
}
