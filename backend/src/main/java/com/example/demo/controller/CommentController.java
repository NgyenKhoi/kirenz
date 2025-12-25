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

    @GetMapping("/post/{postId}")
    public ApiResponse<List<CommentResponse>> getCommentsByPostId(@PathVariable String postId) {
        return ApiResponse.success(commentService.getCommentsByPostId(postId), "Comments retrieved successfully");
    }
    
    @GetMapping("/{id}")
    public ApiResponse<CommentResponse> getCommentById(@PathVariable String id) {
        return ApiResponse.success(commentService.getCommentById(id), "Comment retrieved successfully");
    }

    @PostMapping
    public ApiResponse<CommentResponse> createComment(@RequestBody CreateCommentRequest request) {
        return ApiResponse.success(commentService.createComment(request), "Comment created successfully");
    }

    @PutMapping("/{id}")
    public ApiResponse<CommentResponse> updateComment(
            @PathVariable String id,
            @RequestBody UpdateCommentRequest request) {
        return ApiResponse.success(commentService.updateComment(id, request), "Comment updated successfully");
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteComment(@PathVariable String id) {
        commentService.deleteComment(id);
        return ApiResponse.success("Comment deleted successfully");
    }
}
