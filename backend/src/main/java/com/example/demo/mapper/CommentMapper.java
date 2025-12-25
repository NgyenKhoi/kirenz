package com.example.demo.mapper;

import com.example.demo.document.Comment;
import com.example.demo.dto.response.CommentResponse;
import com.example.demo.dto.request.CreateCommentRequest;
import com.example.demo.dto.request.UpdateCommentRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring")
public interface CommentMapper {
    
    CommentResponse toResponse(Comment comment);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "userId", ignore = true)
    Comment toDocument(CreateCommentRequest request);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "postId", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    void updateDocumentFromRequest(UpdateCommentRequest request, @MappingTarget Comment comment);
    
    List<CommentResponse> toResponseList(List<Comment> comments);
}
