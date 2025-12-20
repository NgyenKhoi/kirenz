package com.example.demo.dto.request;

import lombok.Data;
import java.util.List;

@Data
public class CreatePostRequest {
    // userId is obtained from JWT token, not from request body
    private String content;
    private List<MediaItemRequest> media;
}
