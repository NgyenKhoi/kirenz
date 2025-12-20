package com.example.demo.dto.response;

import lombok.Data;
import java.time.Instant;

@Data
public class CommentResponse {
    private String id;
    private String postId;
    private Integer userId;
    private String content;
    private Instant createdAt;
}
