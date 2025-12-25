package com.example.demo.mapper;

import com.example.demo.document.Post;
import com.example.demo.dto.response.MediaItemResponse;
import com.example.demo.dto.response.PostResponse;
import com.example.demo.dto.request.CreatePostRequest;
import com.example.demo.dto.request.MediaItemRequest;
import com.example.demo.dto.request.UpdatePostRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface PostMapper {

    @Mapping(target = "author", ignore = true)
    PostResponse toResponse(Post post);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "likes", ignore = true)
    @Mapping(target = "commentsCount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "userId", ignore = true)
    Post toDocument(CreatePostRequest request);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "likes", ignore = true)
    @Mapping(target = "commentsCount", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateDocumentFromRequest(UpdatePostRequest request, @MappingTarget Post post);
    
    List<PostResponse> toResponseList(List<Post> posts);
    
    MediaItemResponse toMediaItemResponse(Post.MediaItem mediaItem);
    
    Post.MediaItem toMediaItem(MediaItemRequest mediaItemRequest);
}
