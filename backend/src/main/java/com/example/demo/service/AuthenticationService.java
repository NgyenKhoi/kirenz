package com.example.demo.service;

import com.example.demo.dto.request.AuthRequest;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.entities.User;
import com.example.demo.enums.EntityStatus;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.jpa.UserRepository;
import com.nimbusds.jwt.JWTClaimsSet;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class AuthenticationService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Register a new user
     * @param request Registration request with email and password
     * @return AuthResponse with tokens and user information
     */
    @Transactional
    public AuthResponse register(AuthRequest request) {
        // Validate password length
        if (request.getPassword().length() < 8) {
            throw new AppException(ErrorCode.PASSWORD_TOO_SHORT);
        }

        // Check if user already exists
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        // Create new user
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setIsPremium(false);
        user.setStatus(EntityStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        // Save user to database
        user = userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Build response
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setIsPremium(user.getIsPremium());

        return response;
    }

    /**
     * Login user with email and password
     * @param request Login request with email and password
     * @return AuthResponse with tokens and user information
     */
    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        // Find user by email
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        // Build response
        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setIsPremium(user.getIsPremium());

        return response;
    }

    /**
     * Refresh access token using refresh token with token rotation
     * @param refreshToken Refresh token string
     * @return AuthResponse with new access token and new refresh token
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // Validate refresh token
            JWTClaimsSet claimsSet = jwtService.validateToken(refreshToken);
            
            // Extract user ID from token
            Long userId = Long.parseLong(claimsSet.getSubject());
            
            // Find user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            // Generate NEW access token AND NEW refresh token (Token Rotation)
            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            // Build response with both new tokens
            AuthResponse response = new AuthResponse();
            response.setAccessToken(newAccessToken);
            response.setRefreshToken(newRefreshToken); // Return NEW refresh token
            response.setUserId(user.getId());
            response.setEmail(user.getEmail());
            response.setIsPremium(user.getIsPremium());

            return response;
        } catch (RuntimeException e) {
            // Handle token validation errors
            if (e.getMessage().contains("expired")) {
                throw new AppException(ErrorCode.TOKEN_EXPIRED);
            } else {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }
        }
    }
}
