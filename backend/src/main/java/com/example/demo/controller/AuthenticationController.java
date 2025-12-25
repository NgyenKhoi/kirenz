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
    
    @PostMapping("/register")
    public ApiResponse<AuthResponse> register(@Valid @RequestBody AuthRequest request) {
        ApiResponse<AuthResponse> response = new ApiResponse<>();
        response.setMessage("User registered successfully");
        response.setResult(authenticationService.register(request));
        return response;
    }
    
    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        ApiResponse<AuthResponse> response = new ApiResponse<>();
        response.setMessage("Login successful");
        response.setResult(authenticationService.login(request));
        return response;
    }
    
    @PostMapping("/refresh")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        ApiResponse<AuthResponse> response = new ApiResponse<>();
        response.setMessage("Token refreshed successfully");
        response.setResult(authenticationService.refreshToken(request.getRefreshToken()));
        return response;
    }
}
