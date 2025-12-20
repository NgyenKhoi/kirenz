package com.example.demo.dto.response;

import lombok.Data;
import java.util.Map;

@Data
public class MediaUploadResponse {
    private String cloudinaryPublicId;
    private String url;
    private String type;
    private Map<String, Object> metadata;
}
