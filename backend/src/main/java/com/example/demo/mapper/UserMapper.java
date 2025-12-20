package com.example.demo.mapper;

import com.example.demo.dto.response.UserResponse;
import com.example.demo.entities.User;
import com.example.demo.entities.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {ProfileMapper.class})
public interface UserMapper {

    @Mapping(target = "profile", ignore = true)
    UserResponse toResponse(User user);
    
    @Mapping(source = "user.id", target = "id")
    @Mapping(source = "user.email", target = "email")
    @Mapping(source = "user.createdAt", target = "createdAt")
    @Mapping(source = "user.updatedAt", target = "updatedAt")
    @Mapping(source = "profile", target = "profile")
    UserResponse toResponse(User user, Profile profile);
    
    List<UserResponse> toResponseList(List<User> users);
}
