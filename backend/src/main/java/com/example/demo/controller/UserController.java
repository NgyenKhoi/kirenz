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
    
    /**
     * Get all users
     * @return ApiResponse with list of users
     */
    @GetMapping
    public ApiResponse<List<UserResponse>> getAllUsers() {
        return ApiResponse.success(userService.getAllUsers(), "Users retrieved successfully");
    }
    
    /**
     * Get user by ID
     * @param id User ID
     * @return ApiResponse with user data
     */
    @GetMapping("/{id}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id), "User retrieved successfully");
    }
    
    /**
     * Get user profile (user with profile data)
     * @param id User ID
     * @return ApiResponse with user and profile data
     */
    @GetMapping("/{id}/profile")
    public ApiResponse<UserResponse> getUserProfile(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserProfile(id), "User profile retrieved successfully");
    }
    
    /**
     * Update user profile
     * @param id User ID
     * @param request Update profile request
     * @return ApiResponse with updated profile data
     */
    @PutMapping("/{id}/profile")
    public ApiResponse<ProfileResponse> updateProfile(
            @PathVariable Long id,
            @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(id, request), "Profile updated successfully");
    }
}
