package com.example.demo.controller;

import com.example.demo.dto.request.MediaUploadRequest;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.MediaUploadResponse;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.service.CloudinaryService;
import com.example.demo.util.MediaValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {
    
    private final CloudinaryService cloudinaryService;
    private final MediaValidator mediaValidator;
    
    @PostMapping("/upload")
    public ApiResponse<MediaUploadResponse> uploadMedia(@RequestBody MediaUploadRequest request) {
        log.info("Uploading media: type={}, fileName={}", request.getType(), request.getFileName());
        
        // Validate media type
        mediaValidator.validateMediaType(request.getType());
        
        // Decode base64 data
        byte[] data;
        try {
            data = Base64.getDecoder().decode(request.getBase64Data());
        } catch (IllegalArgumentException e) {
            log.error("Invalid base64 data: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_MEDIA_FORMAT);
        }
        
        // Validate file data
        mediaValidator.validateFileData(data);
        
        MediaUploadResponse response;
        
        if ("IMAGE".equalsIgnoreCase(request.getType())) {
            // Validate image
            mediaValidator.validateImage(request.getFileName(), data.length);
            response = cloudinaryService.uploadImage(data, request.getFileName());
        } else if ("VIDEO".equalsIgnoreCase(request.getType())) {
            // Validate video
            mediaValidator.validateVideo(request.getFileName(), data.length);
            response = cloudinaryService.uploadVideo(data, request.getFileName());
        } else {
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }
        
        log.info("Media uploaded successfully: publicId={}", response.getCloudinaryPublicId());
        return ApiResponse.success(response, "Media uploaded successfully");
    }
}
