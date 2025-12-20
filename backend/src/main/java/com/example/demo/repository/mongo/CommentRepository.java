package com.example.demo.repository.mongo;

import com.example.demo.document.Comment;
import com.example.demo.enums.EntityStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CommentRepository extends MongoRepository<Comment, String> {
    List<Comment> findByPostIdAndStatusOrderByCreatedAtAsc(String postId, EntityStatus status);
    Optional<Comment> findByIdAndStatus(String id, EntityStatus status);
    List<Comment> findByPostIdAndStatus(String postId, EntityStatus status);
    List<Comment> findByStatusAndDeletedAtBefore(EntityStatus status, Instant deletedAt);
}
