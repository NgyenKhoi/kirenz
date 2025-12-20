package com.example.demo.controller;

import com.example.demo.dto.request.CreateCommentRequest;
import com.example.demo.dto.request.UpdateCommentRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
public class CommentController {
    
    private final CommentService commentService;
    
    /**
     * Get comments by post ID
     * @param postId Post ID
     * @return ApiResponse with list of comments
     */
    @GetMapping("/post/{postId}")
    public ApiResponse<List<CommentResponse>> getCommentsByPostId(@PathVariable String postId) {
        return ApiResponse.success(commentService.getCommentsByPostId(postId), "Comments retrieved successfully");
    }
    
    /**
     * Get comment by ID
     * @param id Comment ID
     * @return ApiResponse with comment data
     */
    @GetMapping("/{id}")
    public ApiResponse<CommentResponse> getCommentById(@PathVariable String id) {
        return ApiResponse.success(commentService.getCommentById(id), "Comment retrieved successfully");
    }
    
    /**
     * Create a new comment
     * @param request Create comment request
     * @return ApiResponse with created comment data
     */
    @PostMapping
    public ApiResponse<CommentResponse> createComment(@RequestBody CreateCommentRequest request) {
        return ApiResponse.success(commentService.createComment(request), "Comment created successfully");
    }
    
    /**
     * Update an existing comment
     * @param id Comment ID
     * @param request Update comment request
     * @return ApiResponse with updated comment data
     */
    @PutMapping("/{id}")
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable String id,
            @RequestBody UpdateCommentRequest request) {
        return ApiResponse.success(commentService.updateComment(id, request), "Comment updated successfully");
    }
    
    /**
     * Delete a comment
     * @param id Comment ID
     * @return ApiResponse with success message
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable String id) {
        commentService.deleteComment(id);
        return ApiResponse.success("Comment deleted successfully");
    }
}
