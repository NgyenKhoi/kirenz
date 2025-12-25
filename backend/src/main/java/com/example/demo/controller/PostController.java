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
    
    @GetMapping
    public ApiResponse<List<PostResponse>> getAllPosts() {
        return ApiResponse.success(postService.getAllPosts(), "Posts retrieved successfully");
    }
    
    @GetMapping("/user/{userId}")
    public ApiResponse<List<PostResponse>> getPostsByUserId(@PathVariable Integer userId) {
        return ApiResponse.success(postService.getPostsByUserId(userId), "Posts retrieved successfully");
    }
    
    @GetMapping("/{identifier}")
    public ApiResponse<PostResponse> getPost(@PathVariable String identifier) {
        // Try to get by slug first (if it contains a dash, likely a slug)
        PostResponse post = identifier.contains("-") 
            ? postService.getPostBySlug(identifier)
            : postService.getPostById(identifier);
        
        return ApiResponse.success(post, "Post retrieved successfully");
    }

    @PostMapping
    public ApiResponse<PostResponse> createPost(@RequestBody CreatePostRequest request) {
        return ApiResponse.success(postService.createPost(request), "Post created successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<PostResponse> updatePost(
            @PathVariable String id,
            @RequestBody UpdatePostRequest request) {
        return ApiResponse.success(postService.updatePost(id, request), "Post updated successfully");
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePost(@PathVariable String id) {
        postService.deletePost(id);
        return ApiResponse.success("Post deleted successfully");
    }
}
