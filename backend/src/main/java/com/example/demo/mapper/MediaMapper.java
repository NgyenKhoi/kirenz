package com.example.demo.mapper;

import com.example.demo.document.Message;
import com.example.demo.dto.response.MediaAttachmentResponse;
import com.example.demo.dto.response.MediaUploadResponse;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MediaMapper {
    
    MediaAttachmentResponse toMediaAttachmentResponse(Message.MediaAttachment mediaAttachment);
    
    List<MediaAttachmentResponse> toMediaAttachmentResponseList(List<Message.MediaAttachment> mediaAttachments);
    
    Message.MediaAttachment toMediaAttachment(MediaUploadResponse mediaUploadResponse);
}
