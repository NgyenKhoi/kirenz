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
    private final ProfileService profileService;


    @Transactional
    public AuthResponse register(AuthRequest request) {
        if (request.getPassword().length() < 8) {
            throw new AppException(ErrorCode.PASSWORD_TOO_SHORT);
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setIsPremium(false);
        user.setStatus(EntityStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());

        user = userRepository.save(user);

        // Tạo profile mặc định ngay sau khi tạo user
        profileService.createDefaultProfile(user);

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

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setUserId(user.getId());
        response.setEmail(user.getEmail());
        response.setIsPremium(user.getIsPremium());

        return response;
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        try {
            JWTClaimsSet claimsSet = jwtService.validateToken(refreshToken);
            
            Long userId = Long.parseLong(claimsSet.getSubject());
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

            String newAccessToken = jwtService.generateAccessToken(user);
            String newRefreshToken = jwtService.generateRefreshToken(user);

            AuthResponse response = new AuthResponse();
            response.setAccessToken(newAccessToken);
            response.setRefreshToken(newRefreshToken);
            response.setUserId(user.getId());
            response.setEmail(user.getEmail());
            response.setIsPremium(user.getIsPremium());

            return response;
        } catch (RuntimeException e) {
            if (e.getMessage().contains("expired")) {
                throw new AppException(ErrorCode.TOKEN_EXPIRED);
            } else {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }
        }
    }
}
