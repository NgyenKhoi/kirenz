package com.example.demo.dto.request;

import lombok.Data;

@Data
public class MediaUploadRequest {
    private String type;
    private String base64Data;
    private String fileName;
    private Long fileSize;
}
