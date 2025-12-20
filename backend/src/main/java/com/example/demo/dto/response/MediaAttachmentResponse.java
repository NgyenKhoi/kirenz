package com.example.demo.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class MediaAttachmentResponse {
    private String type;
    private String cloudinaryPublicId;
    private String url;
    private Map<String, Object> metadata;
}
