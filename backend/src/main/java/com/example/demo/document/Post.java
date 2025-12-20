package com.example.demo.document;

import com.example.demo.enums.EntityStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


import lombok.Data;
import java.time.Instant;
import java.util.List;

@Data
@Document(collection = "posts")
public class Post {

    @Id
    private String id;

    private String slug;
    private Integer userId;
    private String content;
    private List<MediaItem> media;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer likes;
    private Integer commentsCount;
    private EntityStatus status = EntityStatus.ACTIVE;
    private Instant deletedAt;

    @Data
    public static class MediaItem {
        private String type;
        private String url;
    }
}
