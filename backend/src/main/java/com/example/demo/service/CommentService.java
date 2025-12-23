package com.example.demo.service;

import com.example.demo.document.Comment;
import com.example.demo.document.Post;
import com.example.demo.dto.request.CreateCommentRequest;
import com.example.demo.dto.request.UpdateCommentRequest;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.enums.EntityStatus;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.CommentMapper;
import com.example.demo.repository.mongo.CommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    
    private final CommentRepository commentRepository;
    private final CommentMapper commentMapper;
    private final MongoTemplate mongoTemplate;
    
    public List<CommentResponse> getCommentsByPostId(String postId) {
        List<Comment> comments = commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(postId, EntityStatus.ACTIVE);
        return commentMapper.toResponseList(comments);
    }
    
    public CommentResponse getCommentById(String id) {
        Comment comment = commentRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        return commentMapper.toResponse(comment);
    }
    
    public CommentResponse createComment(CreateCommentRequest request) {
        Long currentUserId = com.example.demo.util.SecurityUtils.getCurrentUserId();
        
        Comment comment = commentMapper.toDocument(request);
        comment.setUserId(currentUserId.intValue());
        comment.setCreatedAt(Instant.now());
        comment.setStatus(EntityStatus.ACTIVE);
        
        Comment savedComment = commentRepository.save(comment);
        
        incrementPostCommentCount(request.getPostId());
        
        return commentMapper.toResponse(savedComment);
    }
    
    private void incrementPostCommentCount(String postId) {
        Query query = new Query(Criteria.where("_id").is(postId));
        Update update = new Update().inc("commentsCount", 1);
        mongoTemplate.updateFirst(query, update, Post.class);
    }
    
    public CommentResponse updateComment(String id, UpdateCommentRequest request) {
        Comment comment = commentRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        
        commentMapper.updateDocumentFromRequest(request, comment);
        
        Comment savedComment = commentRepository.save(comment);
        return commentMapper.toResponse(savedComment);
    }
    
    public void deleteComment(String id) {
        Comment comment = commentRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        
        comment.setStatus(EntityStatus.DELETED);
        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);
        decrementPostCommentCount(comment.getPostId());
    }
    
    private void decrementPostCommentCount(String postId) {
        Query query = new Query(Criteria.where("_id").is(postId)
                .and("commentsCount").gt(0));
        Update update = new Update().inc("commentsCount", -1);
        mongoTemplate.updateFirst(query, update, Post.class);
    }
    
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupDeletedComments() {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<Comment> commentsToDelete = commentRepository.findByStatusAndDeletedAtBefore(EntityStatus.DELETED, sevenDaysAgo);
        
        if (!commentsToDelete.isEmpty()) {
            commentRepository.deleteAll(commentsToDelete);
            log.info("Cleaned up {} deleted comments older than 7 days", commentsToDelete.size());
        }
    }
}
