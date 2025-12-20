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
    
    /**
     * Get all active users
     * @return List of UserResponse
     */
    public List<UserResponse> getAllUsers() {
        List<User> users = userRepository.findByStatus(EntityStatus.ACTIVE);
        return userMapper.toResponseList(users);
    }
    
    /**
     * Get user by ID
     * @param id User ID
     * @return UserResponse
     * @throws AppException if user not found
     */
    public UserResponse getUserById(Long id) {
        User user = userRepository.findByIdAndStatus(id, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        return userMapper.toResponse(user);
    }
    
    /**
     * Get user profile (user with profile data)
     * @param userId User ID
     * @return UserResponse with profile data
     * @throws AppException if user not found
     */
    @Transactional
    public UserResponse getUserProfile(Long userId) {
        User user = userRepository.findByIdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Get or create profile if it doesn't exist
        Profile profile = profileRepository.findByUser_IdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseGet(() -> {
                    // Create default profile for user
                    Profile newProfile = new Profile();
                    newProfile.setUser(user);
                    newProfile.setStatus(EntityStatus.ACTIVE);
                    newProfile.setUpdatedAt(Instant.now());
                    return profileRepository.save(newProfile);
                });
        
        return userMapper.toResponse(user, profile);
    }
    
    /**
     * Update user profile
     * @param userId User ID
     * @param request Update profile request
     * @return Updated ProfileResponse
     * @throws AppException if user not found
     */
    @Transactional
    public ProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        // Verify user exists
        User user = userRepository.findByIdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        // Get or create profile if it doesn't exist
        Profile profile = profileRepository.findByUser_IdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseGet(() -> {
                    Profile newProfile = new Profile();
                    newProfile.setUser(user);
                    newProfile.setStatus(EntityStatus.ACTIVE);
                    return newProfile;
                });
        
        profileMapper.updateEntityFromRequest(request, profile);
        profile.setUpdatedAt(Instant.now());
        
        Profile savedProfile = profileRepository.save(profile);
        return profileMapper.toResponse(savedProfile);
    }
    
    /**
     * Soft delete a user and their profile
     * @param userId User ID
     * @throws AppException if user not found
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findByIdAndStatus(userId, EntityStatus.ACTIVE)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
        
        Instant now = Instant.now();
        user.setStatus(EntityStatus.DELETED);
        user.setDeletedAt(now);
        userRepository.save(user);
        
        // Also soft delete the profile
        profileRepository.findByUser_IdAndStatus(userId, EntityStatus.ACTIVE)
                .ifPresent(profile -> {
                    profile.setStatus(EntityStatus.DELETED);
                    profile.setDeletedAt(now);
                    profileRepository.save(profile);
                });
    }
    
    /**
     * Cleanup job to permanently delete users and profiles marked as deleted for more than 7 days
     * Runs daily at 3 AM
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupDeletedUsers() {
        Instant sevenDaysAgo = Instant.now().minus(7, ChronoUnit.DAYS);
        
        // Clean up profiles first (due to foreign key constraint)
        List<Profile> profilesToDelete = profileRepository.findByStatusAndDeletedAtBefore(EntityStatus.DELETED, sevenDaysAgo);
        if (!profilesToDelete.isEmpty()) {
            profileRepository.deleteAll(profilesToDelete);
            log.info("Cleaned up {} deleted profiles older than 7 days", profilesToDelete.size());
        }
        
        // Then clean up users
        List<User> usersToDelete = userRepository.findByStatusAndDeletedAtBefore(EntityStatus.DELETED, sevenDaysAgo);
        if (!usersToDelete.isEmpty()) {
            userRepository.deleteAll(usersToDelete);
            log.info("Cleaned up {} deleted users older than 7 days", usersToDelete.size());
        }
    }
}
