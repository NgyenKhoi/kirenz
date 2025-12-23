package com.example.demo.util;

import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class MediaValidator {
    
    @Value("${chat.media.max-image-size:10485760}")
    private long maxImageSize;
    
    @Value("${chat.media.max-video-size:1073741824}")
    private long maxVideoSize;
    
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );
    
    private static final List<String> ALLOWED_VIDEO_TYPES = Arrays.asList(
        "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo", "video/webm"
    );
    
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
        ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );
    
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList(
        ".mp4", ".mpeg", ".mov", ".avi", ".webm"
    );
    
    public void validateImage(String fileName, long fileSize) {
        log.debug("Validating image: {} (size: {} bytes)", fileName, fileSize);
        
        // Validate file size
        if (fileSize > maxImageSize) {
            log.warn("Image file too large: {} bytes (max: {} bytes)", fileSize, maxImageSize);
            throw new AppException(ErrorCode.MEDIA_TOO_LARGE);
        }
        
        // Validate file extension
        String lowerFileName = fileName.toLowerCase();
        boolean hasValidExtension = ALLOWED_IMAGE_EXTENSIONS.stream()
            .anyMatch(lowerFileName::endsWith);
        
        if (!hasValidExtension) {
            log.warn("Invalid image file extension: {}", fileName);
            throw new AppException(ErrorCode.INVALID_MEDIA_FORMAT);
        }
        
        log.debug("Image validation passed");
    }
    
    public void validateVideo(String fileName, long fileSize) {
        log.debug("Validating video: {} (size: {} bytes)", fileName, fileSize);
        
        // Validate file size
        if (fileSize > maxVideoSize) {
            log.warn("Video file too large: {} bytes (max: {} bytes)", fileSize, maxVideoSize);
            throw new AppException(ErrorCode.MEDIA_TOO_LARGE);
        }
        
        // Validate file extension
        String lowerFileName = fileName.toLowerCase();
        boolean hasValidExtension = ALLOWED_VIDEO_EXTENSIONS.stream()
            .anyMatch(lowerFileName::endsWith);
        
        if (!hasValidExtension) {
            log.warn("Invalid video file extension: {}", fileName);
            throw new AppException(ErrorCode.INVALID_MEDIA_FORMAT);
        }
        
        log.debug("Video validation passed");
    }
    
    public void validateMediaType(String mediaType) {
        if (mediaType == null || 
            (!mediaType.equalsIgnoreCase("IMAGE") && !mediaType.equalsIgnoreCase("VIDEO"))) {
            log.warn("Invalid media type: {}", mediaType);
            throw new AppException(ErrorCode.INVALID_MEDIA_TYPE);
        }
    }
    
    public void validateFileData(byte[] data) {
        if (data == null || data.length == 0) {
            log.warn("File data is null or empty");
            throw new AppException(ErrorCode.INVALID_MEDIA_FORMAT);
        }
    }
}
