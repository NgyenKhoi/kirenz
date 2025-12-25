package com.example.demo.controller;

import com.example.demo.dto.request.UpdateProfileRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers(), "Users retrieved successfully");
    }
    
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id), "User retrieved successfully");
    }

    @GetMapping("/{id}/profile")
    public ApiResponse<UserResponse> getUserProfile(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserProfile(id), "User profile retrieved successfully");
    }
    
    @PutMapping("/{id}/profile")
    public ApiResponse<ProfileResponse> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(id, request), "Profile updated successfully");
    }
}
