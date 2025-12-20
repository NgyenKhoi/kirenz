package com.example.demo.document;

import com.example.demo.enums.EntityStatus;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "comments")
public class Comment {

    @Id
    private String id;

    private String postId;
    private Integer userId;
    private String content;
    private Instant createdAt;
    private EntityStatus status = EntityStatus.ACTIVE;
    private Instant deletedAt;
}
