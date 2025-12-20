package com.example.demo.controller;

import com.example.demo.dto.request.CreatePostRequest;
import com.example.demo.dto.request.UpdatePostRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PostResponse;
import com.example.demo.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {
    
    private final PostService postService;
    
    /**
     * Get all posts
     * @return ApiResponse with list of all posts
     */
    @GetMapping
    public ApiResponse<List<PostResponse>> getAllPosts() {
        return ApiResponse.success(postService.getAllPosts(), "Posts retrieved successfully");
    }
    
    /**
     * Get posts by user ID
     * @param userId User ID
     * @return ApiResponse with list of posts
     */
    @GetMapping("/user/{userId}")
    public ApiResponse<List<PostResponse>> getPostsByUserId(@PathVariable Integer userId) {
        return ApiResponse.success(postService.getPostsByUserId(userId), "Posts retrieved successfully");
    }
    
    /**
     * Get post by ID or slug
     * @param identifier Post ID or slug
     * @return ApiResponse with post data
     */
    @GetMapping("/{identifier}")
    public ApiResponse<PostResponse> getPost(@PathVariable String identifier) {
        // Try to get by slug first (if it contains a dash, likely a slug)
        PostResponse post = identifier.contains("-") 
            ? postService.getPostBySlug(identifier)
            : postService.getPostById(identifier);
        
        return ApiResponse.success(post, "Post retrieved successfully");
    }
    
    /**
     * Create a new post
     * @param request Create post request
     * @return ApiResponse with created post data
     */
    @PostMapping
    public ApiResponse<PostResponse> createPost(@RequestBody CreatePostRequest request) {
        return ApiResponse.success(postService.createPost(request), "Post created successfully");
    }
    
    /**
     * Update an existing post
     * @param id Post ID
     * @param request Update post request
     * @return ApiResponse with updated post data
     */
    @PutMapping("/{id}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable String id,
            @RequestBody UpdatePostRequest request) {
        return ApiResponse.success(postService.updatePost(id, request), "Post updated successfully");
    }
    
    /**
     * Delete a post
     * @param id Post ID
     * @return ApiResponse with success message
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable String id) {
        postService.deletePost(id);
        return ApiResponse.success("Post deleted successfully");
    }
}
