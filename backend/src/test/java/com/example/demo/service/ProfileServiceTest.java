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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProfileMapper profileMapper;

    @InjectMocks
    private ProfileService profileService;

    private User user;
    private Profile profile;
    private UpdateProfileRequest updateRequest;
    private ProfileResponse profileResponse;

    @BeforeEach
    void setUp() {
        user = createUser(1L, "test@example.com");
        profile = createProfile(user, "Test User");
        
        updateRequest = new UpdateProfileRequest();
        updateRequest.setFullName("Updated Name");
        updateRequest.setBio("Updated bio");
        updateRequest.setLocation("New York");
        updateRequest.setWebsite("https://example.com");
        updateRequest.setDateOfBirth(LocalDate.of(1990, 1, 1));

        profileResponse = new ProfileResponse();
        profileResponse.setId(1L);
        profileResponse.setFullName("Test User");
    }

    @Test
    void getProfile_ExistingProfile_Success() {
        when(userRepository.findByIdAndStatus(user.getId(), EntityStatus.ACTIVE))
                .thenReturn(Optional.of(user));
        when(profileRepository.findByUser_Id(user.getId()))
                .thenReturn(Optional.of(profile));
        when(profileMapper.toResponse(profile)).thenReturn(profileResponse);

        ProfileResponse result = profileService.getProfile(user.getId());

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        verify(profileMapper).toResponse(profile);
    }

    @Test
    void getProfile_NoExistingProfile_ThrowsException() {
        when(userRepository.findByIdAndStatus(user.getId(), EntityStatus.ACTIVE))
                .thenReturn(Optional.of(user));
        when(profileRepository.findByUser_Id(user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(user.getId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROFILE_NOT_FOUND);

        verify(profileRepository, never()).save(any(Profile.class));
    }

    @Test
    void getProfile_UserNotFound_ThrowsException() {
        when(userRepository.findByIdAndStatus(user.getId(), EntityStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.getProfile(user.getId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void updateProfile_Success() {
        when(userRepository.findByIdAndStatus(user.getId(), EntityStatus.ACTIVE))
                .thenReturn(Optional.of(user));
        when(profileRepository.findByUser_Id(user.getId()))
                .thenReturn(Optional.of(profile));
        when(profileRepository.save(profile)).thenReturn(profile);
        when(profileMapper.toResponse(profile)).thenReturn(profileResponse);

        ProfileResponse result = profileService.updateProfile(user.getId(), updateRequest);

        assertThat(result).isNotNull();
        verify(profileMapper).updateEntityFromRequest(updateRequest, profile);
        verify(profileRepository).save(profile);
    }

    @Test
    void updateProfile_NoExistingProfile_ThrowsException() {
        when(userRepository.findByIdAndStatus(user.getId(), EntityStatus.ACTIVE))
                .thenReturn(Optional.of(user));
        when(profileRepository.findByUser_Id(user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateProfile(user.getId(), updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROFILE_NOT_FOUND);

        verify(profileRepository, never()).save(any(Profile.class));
        verify(profileMapper, never()).updateEntityFromRequest(any(), any());
    }

    @Test
    void createDefaultProfile_Success() {
        when(profileRepository.save(any(Profile.class))).thenReturn(profile);

        Profile result = profileService.createDefaultProfile(user);

        assertThat(result).isNotNull();
        verify(profileRepository).save(any(Profile.class));
        
        ArgumentCaptor<Profile> profileCaptor = ArgumentCaptor.forClass(Profile.class);
        verify(profileRepository).save(profileCaptor.capture());
        Profile savedProfile = profileCaptor.getValue();
        
        assertThat(savedProfile.getUser()).isEqualTo(user);
        assertThat(savedProfile.getFullName()).isEqualTo(user.getEmail().split("@")[0]);
        assertThat(savedProfile.getStatus()).isEqualTo(EntityStatus.ACTIVE);
        assertThat(savedProfile.getCreatedAt()).isNotNull();
        assertThat(savedProfile.getUpdatedAt()).isNotNull();
    }

    @Test
    void updateProfile_UserNotFound_ThrowsException() {
        when(userRepository.findByIdAndStatus(user.getId(), EntityStatus.ACTIVE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateProfile(user.getId(), updateRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);
    }

    @Test
    void deleteProfile_Success() {
        when(profileRepository.findByUser_Id(user.getId()))
                .thenReturn(Optional.of(profile));

        profileService.deleteProfile(user.getId());

        assertThat(profile.getStatus()).isEqualTo(EntityStatus.DELETED);
        assertThat(profile.getDeletedAt()).isNotNull();
        verify(profileRepository).save(profile);
    }

    @Test
    void deleteProfile_ProfileNotFound_ThrowsException() {
        when(profileRepository.findByUser_Id(user.getId()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.deleteProfile(user.getId()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PROFILE_NOT_FOUND);
    }

    private User createUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setStatus(EntityStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        return user;
    }

    private Profile createProfile(User user, String fullName) {
        Profile profile = new Profile();
        profile.setId(1L);
        profile.setUser(user);
        profile.setFullName(fullName);
        profile.setStatus(EntityStatus.ACTIVE);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());
        return profile;
    }
}