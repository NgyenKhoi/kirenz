package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.BeforeEach; 
import com.example.demo.repository.jpa.UserRepository;
import com.example.demo.entities.User;
import com.example.demo.dto.response.UserResponse;
import java.util.Optional;
import com.example.demo.enums.EntityStatus;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.UserMapper;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.any;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    UserMapper userMapper;

    @InjectMocks
    private UserService userService;

    private User user;
    private UserResponse userResponse;

    @BeforeEach
    void setup() {
        user = new User();
        user.setId(1L);
        user.setEmail("test@gmail.com");

        userResponse = new UserResponse();
        userResponse.setId(1L);
    }

    @Test
    void getUserById_success() {

        when(userRepository.findByIdAndStatus(1L, EntityStatus.ACTIVE))
        .thenReturn(Optional.of(user));
        when(userMapper.toResponse(user))
        .thenReturn(userResponse);

        UserResponse result = userService.getUserById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);

        verify(userMapper).toResponse(user);
    }

    @Test
    void getUserById_NoExistingUser_ThrowsException() {
        when(userRepository.findByIdAndStatus(user.getId(), EntityStatus.ACTIVE))
        .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(user.getId()))
                                .isInstanceOf(AppException.class)
                                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository, never()).save(any(User.class));
    }
}