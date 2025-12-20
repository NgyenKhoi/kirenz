package com.example.demo.service;

import com.example.demo.document.Post;
import com.example.demo.dto.request.CreatePostRequest;
import com.example.demo.dto.request.UpdatePostRequest;
import com.example.demo.dto.response.PostResponse;
import com.example.demo.enums.EntityStatus;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.PostMapper;
import com.example.demo.repository.mongo.PostRepository;
import com.example.demo.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostService {
    
    private final PostRepository postRepository;
    private final PostMapper postMapper;
    private final com.example.demo.repository.jpa.UserRepository userRepository;
    
    /**
     * Get all posts
     * @return List of PostResponse
     */
    public List<PostResponse> getAllPosts() {
        List<Post> posts = postRepository.findByStatusOrderByCreatedAtDesc(EntityStatus.ACTIVE);
        return postMapper.toResponseList(posts);
    }
    
    /**
     * Get posts by user ID
     * @param userId User ID
     * @return List of PostResponse
     */
    public List<PostResponse> getPostsByUserId(Integer userId) {
        List<Post> posts = postRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, EntityStatus.ACTIVE);
        return postMapper.toResponseList(posts);
    }
    
    /**
     * Get post by ID
     * @param id Post ID
     * @return PostResponse
     * @throws AppException if post not found
     */
    public PostResponse getPostById(String id) {
        Post post = postRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        return postMapper.toResponse(post);
    }
    
    /**
     * Get post by slug
     * @param slug Post slug
     * @return PostResponse
     * @throws AppException if post not found
     */
    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlugAndStatus(slug, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        return postMapper.toResponse(post);
    }
    
    /**
     * Create a new post
     * @param request Create post request
     * @return Created PostResponse
     */
    public PostResponse createPost(CreatePostRequest request) {
        // Get current user ID from security context
        Long currentUserId = com.example.demo.util.SecurityUtils.getCurrentUserId();
        
        // Validate user exists
        userRepository.findByIdAndStatus(currentUserId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Post post = postMapper.toDocument(request);
        // Override userId with current authenticated user
        post.setUserId(currentUserId.intValue());
        
        Instant now = Instant.now();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        post.setLikes(0);
        post.setCommentsCount(0);
        post.setStatus(EntityStatus.ACTIVE);
        
        // Generate slug from content preview and timestamp (before saving)
        String contentPreview = extractContentPreview(post.getContent());
        String slug = SlugUtil.generateHybridSlug(contentPreview, String.valueOf(now.toEpochMilli()));
        post.setSlug(slug);
        
        try {
            // Save post
            Post savedPost = postRepository.save(post);
            
            // Convert to response - if this fails, rollback by deleting the saved post
            PostResponse response = postMapper.toResponse(savedPost);
            return response;
        } catch (Exception e) {
            // If anything fails after save, delete the created post to maintain consistency
            if (post.getId() != null) {
                try {
                    postRepository.deleteById(post.getId());
                } catch (Exception rollbackEx) {
                    log.error("Failed to rollback post creation: {}", rollbackEx.getMessage());
                }
            }
            throw e;
        }
    }
    
    /**
     * Extract a preview from post content for slug generation
     * @param content Post content
     * @return Content preview (first 50 chars)
     */
    private String extractContentPreview(String content) {
        if (content == null || content.isEmpty()) {
            return "post";
        }
        
        // Remove HTML tags if any
        String text = content.replaceAll("<[^>]*>", "");
        
        // Take first 50 characters
        if (text.length() > 50) {
            text = text.substring(0, 50);
        }
        
        return text;
    }
    
    /**
     * Update an existing post
     * @param id Post ID
     * @param request Update post request
     * @return Updated PostResponse
     * @throws AppException if post not found
     */
    public PostResponse updatePost(String id, UpdatePostRequest request) {
        Post post = postRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        
        postMapper.updateDocumentFromRequest(request, post);
        post.setUpdatedAt(Instant.now());
        
        // Regenerate slug if content changed
        if (request.getContent() != null && !request.getContent().equals(post.getContent())) {
            String contentPreview = extractContentPreview(request.getContent());
            String slug = SlugUtil.generateHybridSlug(contentPreview, post.getId());
            post.setSlug(slug);
        }
        
        Post savedPost = postRepository.save(post);
        return postMapper.toResponse(savedPost);
    }
    
    /**
     * Soft delete a post
     * @param id Post ID
     * @throws AppException if post not found
     */
    public void deletePost(String id) {
        Post post = postRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.POST_NOT_FOUND));
        
        post.setStatus(EntityStatus.DELETED);
        post.setDeletedAt(Instant.now());
        postRepository.save(post);
    }
    
    /**
     * Cleanup job to permanently delete posts marked as deleted for more than 7 days
     * Runs daily at 2 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupDeletedPosts() {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        List<Post> postsToDelete = postRepository.findByStatusAndDeletedAtBefore(EntityStatus.DELETED, sevenDaysAgo);
        
        if (!postsToDelete.isEmpty()) {
            postRepository.deleteAll(postsToDelete);
            log.info("Cleaned up {} deleted posts older than 7 days", postsToDelete.size());
        }
    }
}
