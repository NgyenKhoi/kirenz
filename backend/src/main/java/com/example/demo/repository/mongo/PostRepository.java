package com.example.demo.repository.mongo;

import com.example.demo.document.Post;
import com.example.demo.enums.EntityStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PostRepository extends MongoRepository<Post, String> {
    List<Post> findByStatusOrderByCreatedAtDesc(EntityStatus status);
    List<Post> findByUserIdAndStatusOrderByCreatedAtDesc(Integer userId, EntityStatus status);
    Optional<Post> findByIdAndStatus(String id, EntityStatus status);
    Optional<Post> findBySlugAndStatus(String slug, EntityStatus status);
    List<Post> findByStatusAndDeletedAtBefore(EntityStatus status, Instant deletedAt);
}
