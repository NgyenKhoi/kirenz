package com.example.demo.mapper;

import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.dto.request.UpdateProfileRequest;
import com.example.demo.entities.Profile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
    
    ProfileResponse toResponse(Profile profile);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    Profile toEntity(UpdateProfileRequest request);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateEntityFromRequest(UpdateProfileRequest request, @MappingTarget Profile profile);
}
