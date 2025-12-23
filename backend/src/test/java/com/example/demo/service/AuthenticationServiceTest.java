package com.example.demo.service;

import com.example.demo.dto.request.AuthRequest;
import com.example.demo.dto.response.AuthResponse;
import com.example.demo.entities.Profile;
import com.example.demo.entities.User;
import com.example.demo.enums.EntityStatus;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private JwtService jwtService;
    
    @Mock
    private ProfileService profileService;
    
    @InjectMocks
    private AuthenticationService authenticationService;
    
    private AuthRequest authRequest;
    private User user;
    private Profile profile;
    
    @BeforeEach
    void setUp() {
        authRequest = new AuthRequest();
        authRequest.setEmail("test@example.com");
        authRequest.setPassword("password123");
        
        user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setPasswordHash("hashedPassword");
        user.setIsPremium(false);
        user.setStatus(EntityStatus.ACTIVE);
        user.setCreatedAt(Instant.now());
        user.setUpdatedAt(Instant.now());
        
        profile = new Profile();
        profile.setId(1L);
        profile.setUser(user);
        profile.setFullName("test");
        profile.setStatus(EntityStatus.ACTIVE);
        profile.setCreatedAt(Instant.now());
        profile.setUpdatedAt(Instant.now());
    }
    
    @Test
    void register_Success_CreatesUserAndProfile() {
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(authRequest.getPassword())).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(profileService.createDefaultProfile(any(User.class))).thenReturn(profile);
        when(jwtService.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken");
        
        AuthResponse result = authenticationService.register(authRequest);
        
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("accessToken");
        assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getIsPremium()).isEqualTo(user.getIsPremium());
        
        verify(userRepository).findByEmail(authRequest.getEmail());
        verify(passwordEncoder).encode(authRequest.getPassword());
        verify(userRepository).save(any(User.class));
        verify(profileService).createDefaultProfile(any(User.class));
        verify(jwtService).generateAccessToken(user);
        verify(jwtService).generateRefreshToken(user);
    }
    
    @Test
    void register_PasswordTooShort_ThrowsException() {
        authRequest.setPassword("short");
        
        assertThatThrownBy(() -> authenticationService.register(authRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PASSWORD_TOO_SHORT);
    }
    
    @Test
    void register_UserAlreadyExists_ThrowsException() {
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.of(user));
        
        assertThatThrownBy(() -> authenticationService.register(authRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_ALREADY_EXISTS);
    }
    
    @Test
    void login_Success() {
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(authRequest.getPassword(), user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(user)).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(user)).thenReturn("refreshToken");
        
        AuthResponse result = authenticationService.login(authRequest);
        
        assertThat(result).isNotNull();
        assertThat(result.getAccessToken()).isEqualTo("accessToken");
        assertThat(result.getRefreshToken()).isEqualTo("refreshToken");
        assertThat(result.getUserId()).isEqualTo(user.getId());
        assertThat(result.getEmail()).isEqualTo(user.getEmail());
        assertThat(result.getIsPremium()).isEqualTo(user.getIsPremium());
    }
    
    @Test
    void login_UserNotFound_ThrowsException() {
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.empty());
        
        assertThatThrownBy(() -> authenticationService.login(authRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }
    
    @Test
    void login_InvalidPassword_ThrowsException() {
        when(userRepository.findByEmail(authRequest.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(authRequest.getPassword(), user.getPasswordHash())).thenReturn(false);
        
        assertThatThrownBy(() -> authenticationService.login(authRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CREDENTIALS);
    }
}