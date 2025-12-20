package com.example.demo.controller;

import com.example.demo.dto.request.AuthRequest;
import com.example.demo.dto.request.RefreshTokenRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {
    
    private final AuthenticationService authenticationService;
    
    /**
     * Register a new user
     * @param request Registration request with email and password
     * @return ApiResponse with authentication tokens and user information
     */
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        ApiResponse<AuthResponse> response = new ApiResponse<>();
        response.setMessage("User registered successfully");
        response.setResult(authenticationService.register(request));
        return response;
    }
    
    /**
     * Login user with email and password
     * @param request Login request with email and password
     * @return ApiResponse with authentication tokens and user information
     */
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        ApiResponse<AuthResponse> response = new ApiResponse<>();
        response.setMessage("Login successful");
        response.setResult(authenticationService.login(request));
        return response;
    }
    
    /**
     * Refresh access token using refresh token
     * @param request Refresh token request
     * @return ApiResponse with new access token
     */
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        ApiResponse<AuthResponse> response = new ApiResponse<>();
        response.setMessage("Token refreshed successfully");
        response.setResult(authenticationService.refreshToken(request.getRefreshToken()));
        return response;
    }
}
