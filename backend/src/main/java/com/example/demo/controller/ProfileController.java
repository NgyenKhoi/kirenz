package com.example.demo.controller;

import com.example.demo.dto.request.UpdateProfileRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.service.ProfileService;
import com.example.demo.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    
    private final ProfileService profileService;
    
    @GetMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        ProfileResponse profile = profileService.getProfile(userId);
        
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
    
    @PutMapping
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        ProfileResponse profile = profileService.updateProfile(userId, request);
        
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
    
    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> deleteProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        profileService.deleteProfile(userId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}