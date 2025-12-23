package com.example.demo.service;

import com.example.demo.dto.request.UpdateProfileRequest;
import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.entities.Profile;
import com.example.demo.entities.User;
import com.example.demo.enums.EntityStatus;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.ProfileMapper;
import com.example.demo.repository.jpa.ProfileRepository;
import com.example.demo.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileService {
    
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ProfileMapper profileMapper;
    
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        userRepository.findByIdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Profile profile = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));
        
        return profileMapper.toResponse(profile);
    }
    
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        userRepository.findByIdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Profile profile = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));
        
        profileMapper.updateEntityFromRequest(request, profile);
        profile.setUpdatedAt(Instant.now());
        
        Profile savedProfile = profileRepository.save(profile);
        log.info("Updated profile for user {}", userId);
        
        return profileMapper.toResponse(savedProfile);
    }
    
    @Transactional
    public void deleteProfile(Long userId) {
        Profile profile = profileRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));
        
        profile.setStatus(EntityStatus.DELETED);
        profile.setDeletedAt(Instant.now());
        profileRepository.save(profile);
        
        log.info("Deleted profile for user {}", userId);
    }
    
    @Transactional
    public Profile createDefaultProfile(User user) {
        Profile profile = new Profile();
        profile.setUser(user);
        profile.setFullName(user.getEmail().split("@")[0]);
        profile.setStatus(EntityStatus.ACTIVE);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());
        
        return profileRepository.save(profile);
    }
}
