package com.example.demo.dto.response;

import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
public class PostResponse {
    private String id;
    private String slug;
    private Integer userId;
    private PostAuthorResponse author; // Author info to avoid N+1 queries
    private String content;
    private List<MediaItemResponse> media;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer likes;
    private Integer commentsCount;
    
    @Data
    public static class PostAuthorResponse {
        private Integer id;
        private String email;
        private String fullName;
        private String avatarUrl;
    }
}
