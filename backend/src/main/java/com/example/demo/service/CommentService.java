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
    
    /**
     * Get comments by post ID
     * @param postId Post ID
     * @return List of CommentResponse
     */
    public List<CommentResponse> getCommentsByPostId(String postId) {
        List<Comment> comments = commentRepository.findByPostIdAndStatusOrderByCreatedAtAsc(postId, EntityStatus.ACTIVE);
        return commentMapper.toResponseList(comments);
    }
    
    /**
     * Get comment by ID
     * @param id Comment ID
     * @return CommentResponse
     * @throws AppException if comment not found
     */
    public CommentResponse getCommentById(String id) {
        Comment comment = commentRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        return commentMapper.toResponse(comment);
    }
    
    /**
     * Create a new comment
     * @param request Create comment request
     * @return Created CommentResponse
     */
    public CommentResponse createComment(CreateCommentRequest request) {
        // Get current user ID from security context
        Long currentUserId = com.example.demo.util.SecurityUtils.getCurrentUserId();
        
        Comment comment = commentMapper.toDocument(request);
        // Override userId with current authenticated user
        comment.setUserId(currentUserId.intValue());
        comment.setCreatedAt(Instant.now());
        comment.setStatus(EntityStatus.ACTIVE);
        
        Comment savedComment = commentRepository.save(comment);
        
        // Increment comment count on post
        incrementPostCommentCount(request.getPostId());
        
        return commentMapper.toResponse(savedComment);
    }
    
    /**
     * Increment comment count for a post using atomic $inc operation
     * @param postId Post ID
     */
    private void incrementPostCommentCount(String postId) {
        Query query = new Query(Criteria.where("_id").is(postId));
        Update update = new Update().inc("commentsCount", 1);
        mongoTemplate.updateFirst(query, update, Post.class);
    }
    
    /**
     * Update an existing comment
     * @param id Comment ID
     * @param request Update comment request
     * @return Updated CommentResponse
     * @throws AppException if comment not found
     */
    public CommentResponse updateComment(String id, UpdateCommentRequest request) {
        Comment comment = commentRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        
        commentMapper.updateDocumentFromRequest(request, comment);
        
        Comment savedComment = commentRepository.save(comment);
        return commentMapper.toResponse(savedComment);
    }
    
    /**
     * Soft delete a comment
     * @param id Comment ID
     * @throws AppException if comment not found
     */
    public void deleteComment(String id) {
        Comment comment = commentRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.COMMENT_NOT_FOUND));
        
        comment.setStatus(EntityStatus.DELETED);
        comment.setDeletedAt(Instant.now());
        commentRepository.save(comment);
        // Decrement comment count on post
        decrementPostCommentCount(comment.getPostId());
    }
    
    /**
     * Decrement comment count for a post using atomic $inc operation
     * @param postId Post ID
     */
    private void decrementPostCommentCount(String postId) {
        Query query = new Query(Criteria.where("_id").is(postId)
                .and("commentsCount").gt(0));
        Update update = new Update().inc("commentsCount", -1);
        mongoTemplate.updateFirst(query, update, Post.class);
    }
    
    /**
     * Cleanup job to permanently delete comments marked as deleted for more than 7 days
     * Runs daily at 2 AM
     */
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
