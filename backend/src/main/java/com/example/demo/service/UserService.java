package com.example.demo.service;

import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.dto.request.UpdateProfileRequest;
import com.example.demo.entities.Profile;
import com.example.demo.entities.User;
import com.example.demo.enums.EntityStatus;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.ProfileMapper;
import com.example.demo.mapper.UserMapper;
import com.example.demo.repository.jpa.ProfileRepository;
import com.example.demo.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final UserMapper userMapper;
    private final ProfileMapper profileMapper;

    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findByStatus(EntityStatus.ACTIVE);
        return userMapper.toResponseList(users);
    }

    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }
    
    @Transactional
    public UserResponse getUserProfile(Long userId) {
        User user = userRepository.findByIdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Profile profile = profileRepository.findByUser_IdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));
        
        return userMapper.toResponse(user, profile);
    }
    
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        userRepository.findByIdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Profile profile = profileRepository.findByUser_IdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.PROFILE_NOT_FOUND));
        
        profileMapper.updateEntityFromRequest(request, profile);
        profile.setUpdatedAt(Instant.now());
        
        Profile savedProfile = profileRepository.save(profile);
        return profileMapper.toResponse(savedProfile);
    }
    
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findByIdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Instant now = Instant.now();
        user.setStatus(EntityStatus.DELETED);
        user.setDeletedAt(now);
        userRepository.save(user);
        
        profileRepository.findByUser_IdAndStatus(userId, EntityStatus.ACTIVE)
                .ifPresent(profile -> {
                    profile.setStatus(EntityStatus.DELETED);
                    profile.setDeletedAt(now);
                    profileRepository.save(profile);
                });
    }
    
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupDeletedUsers() {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        
        List<Profile> profilesToDelete = profileRepository.findByStatusAndDeletedAtBefore(EntityStatus.DELETED, sevenDaysAgo);
        if (!profilesToDelete.isEmpty()) {
            profileRepository.deleteAll(profilesToDelete);
            log.info("Cleaned up {} deleted profiles older than 7 days", profilesToDelete.size());
        }
        
        List<User> usersToDelete = userRepository.findByStatusAndDeletedAtBefore(EntityStatus.DELETED, sevenDaysAgo);
        if (!usersToDelete.isEmpty()) {
            userRepository.deleteAll(usersToDelete);
            log.info("Cleaned up {} deleted users older than 7 days", usersToDelete.size());
        }
    }
}
