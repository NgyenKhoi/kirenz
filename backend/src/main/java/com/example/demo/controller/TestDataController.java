package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.entities.Profile;
import com.example.demo.entities.User;
import com.example.demo.enums.EntityStatus;
import com.example.demo.repository.jpa.ProfileRepository;
import com.example.demo.repository.jpa.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestDataController {
    
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PasswordEncoder passwordEncoder;
    
    @PostMapping("/seed-users")
    @Transactional
    public ApiResponse<String> seedTestUsers() {
        log.info("Seeding test users with correct password hashes");
        
        String password = "password123";
        String passwordHash = passwordEncoder.encode(password);
        
        List<String> createdUsers = new ArrayList<>();
        
        // User 1: Sarah Johnson
        if (!userRepository.findByEmail("sarah.johnson@example.com").isPresent()) {
            User sarah = new User();
            sarah.setEmail("sarah.johnson@example.com");
            sarah.setPasswordHash(passwordHash);
            sarah.setIsPremium(false);
            sarah.setStatus(EntityStatus.ACTIVE);
            sarah.setCreatedAt(Instant.now().minusSeconds(15552000)); // 6 months ago
            sarah.setUpdatedAt(Instant.now());
            sarah = userRepository.save(sarah);
            
            Profile sarahProfile = new Profile();
            sarahProfile.setUser(sarah);
            sarahProfile.setFullName("Sarah Johnson");
            sarahProfile.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=Sarah");
            sarahProfile.setBio("Digital artist & designer 🎨 Creating beautiful things");
            sarahProfile.setBirthday(LocalDate.of(1995, 3, 15));
            sarahProfile.setUpdatedAt(Instant.now());
            profileRepository.save(sarahProfile);
            
            createdUsers.add("sarah.johnson@example.com");
        }
        
        // User 2: Mike Chen
        if (!userRepository.findByEmail("mike.chen@example.com").isPresent()) {
            User mike = new User();
            mike.setEmail("mike.chen@example.com");
            mike.setPasswordHash(passwordHash);
            mike.setIsPremium(false);
            mike.setStatus(EntityStatus.ACTIVE);
            mike.setCreatedAt(Instant.now().minusSeconds(10368000)); // 4 months ago
            mike.setUpdatedAt(Instant.now());
            mike = userRepository.save(mike);
            
            Profile mikeProfile = new Profile();
            mikeProfile.setUser(mike);
            mikeProfile.setFullName("Mike Chen");
            mikeProfile.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=Mike");
            mikeProfile.setBio("Software engineer | Coffee enthusiast ☕");
            mikeProfile.setBirthday(LocalDate.of(1992, 7, 22));
            mikeProfile.setUpdatedAt(Instant.now());
            profileRepository.save(mikeProfile);
            
            createdUsers.add("mike.chen@example.com");
        }
        
        // User 3: Emma Wilson
        if (!userRepository.findByEmail("emma.wilson@example.com").isPresent()) {
            User emma = new User();
            emma.setEmail("emma.wilson@example.com");
            emma.setPasswordHash(passwordHash);
            emma.setIsPremium(false);
            emma.setStatus(EntityStatus.ACTIVE);
            emma.setCreatedAt(Instant.now().minusSeconds(7776000)); // 3 months ago
            emma.setUpdatedAt(Instant.now());
            emma = userRepository.save(emma);
            
            Profile emmaProfile = new Profile();
            emmaProfile.setUser(emma);
            emmaProfile.setFullName("Emma Wilson");
            emmaProfile.setAvatarUrl("https://api.dicebear.com/7.x/avataaars/svg?seed=Emma");
            emmaProfile.setBio("Travel photographer 📸 Capturing moments around the world");
            emmaProfile.setBirthday(LocalDate.of(1998, 11, 8));
            emmaProfile.setUpdatedAt(Instant.now());
            profileRepository.save(emmaProfile);
            
            createdUsers.add("emma.wilson@example.com");
        }
        
        if (createdUsers.isEmpty()) {
            return ApiResponse.success("All test users already exist. Password: " + password);
        }
        
        return ApiResponse.success(
            "Created " + createdUsers.size() + " test users: " + String.join(", ", createdUsers) + 
            ". Password for all: " + password
        );
    }
}
