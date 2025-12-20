package com.example.demo.controller;

import com.example.demo.dto.request.CreateConversationRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.ConversationResponse;
import com.example.demo.dto.response.MessageResponse;
import com.example.demo.dto.response.UserPresenceResponse;
import com.example.demo.service.ChatService;
import com.example.demo.service.UserPresenceService;
import com.example.demo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {
    
    private final ChatService chatService;
    private final UserPresenceService presenceService;
    
    @PostMapping("/conversations")
    public ApiResponse<ConversationResponse> createConversation(
            @RequestBody CreateConversationRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(
            chatService.createConversation(request, userId),
            "Conversation created successfully"
        );
    }
    
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> getUserConversations() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(
            chatService.getUserConversations(userId),
            "Conversations retrieved successfully"
        );
    }
    
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable String id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ApiResponse.success(
            chatService.getMessages(id, userId, page, size),
            "Messages retrieved successfully"
        );
    }
    
    @PostMapping("/conversations/{id}/read")
    public ApiResponse<Void> markAsRead(@PathVariable String id) {
        Long userId = SecurityUtils.getCurrentUserId();
        chatService.markAsRead(id, userId);
        return ApiResponse.success("Messages marked as read");
    }
    
    @GetMapping("/conversations/{id}/presence")
    public ApiResponse<List<UserPresenceResponse>> getOnlineUsers(@PathVariable String id) {
        return ApiResponse.success(
            presenceService.getOnlineUsers(id),
            "Online users retrieved successfully"
        );
    }
    
    @GetMapping("/presence")
    public ApiResponse<List<UserPresenceResponse>> getAllUserPresence() {
        return ApiResponse.success(
            presenceService.getAllUserPresence(),
            "User presence retrieved successfully"
        );
    }
}
