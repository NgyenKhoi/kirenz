package com.example.demo.dto.request;

import lombok.Data;

@Data
public class CreateCommentRequest {
    private String postId;
    // userId is obtained from JWT token, not from request body
    private String content;
}
