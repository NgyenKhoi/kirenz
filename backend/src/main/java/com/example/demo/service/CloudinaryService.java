package com.example.demo.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.demo.dto.response.MediaUploadResponse;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryService {
    
    private final Cloudinary cloudinary;
    
    private static final long CHUNK_SIZE = 50 * 1024 * 1024; // 50MB
    private static final long VIDEO_CHUNK_THRESHOLD = 100 * 1024 * 1024; // 100MB
    private static final long STREAMING_THRESHOLD = 10 * 1024 * 1024; // 10MB
    
    public MediaUploadResponse uploadImage(byte[] data, String fileName) {
        try {
            Map<String, Object> uploadParams = ObjectUtils.asMap(
                "resource_type", "image",
                "folder", "chat/images",
                "use_filename", true,
                "unique_filename", true
            );
            
            @SuppressWarnings("unchecked")
            Map<String, Object> uploadResult = cloudinary.uploader().upload(data, uploadParams);
            
            return buildMediaUploadResponse(uploadResult, "IMAGE", data.length);
        } catch (IOException e) {
            log.error("Failed to upload image: {}", e.getMessage());
            throw new AppException(ErrorCode.MEDIA_UPLOAD_FAILED);
        }
    }
    
    public MediaUploadResponse uploadVideo(byte[] data, String fileName) {
        try {
            long fileSize = data.length;
            
            if (fileSize < VIDEO_CHUNK_THRESHOLD) {
                return uploadVideoSingle(data, fileName);
            } else {
                return uploadVideoChunked(data, fileName);
            }
        } catch (Exception e) {
            log.error("Failed to upload video: {}", e.getMessage());
            throw new AppException(ErrorCode.MEDIA_UPLOAD_FAILED);
        }
    }
    
    private MediaUploadResponse uploadVideoSingle(byte[] data, String fileName) throws IOException {
        Map<String, Object> uploadParams = ObjectUtils.asMap(
            "resource_type", "video",
            "folder", "chat/videos",
            "use_filename", true,
            "unique_filename", true
        );
        
        if (data.length >= STREAMING_THRESHOLD) {
            uploadParams.put("chunk_size", 6000000); // 6MB chunks for streaming
        }
        
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(data, uploadParams);
        
        return buildMediaUploadResponse(uploadResult, "VIDEO", data.length);
    }
    
    private MediaUploadResponse uploadVideoChunked(byte[] data, String fileName) throws IOException {
        String uploadId = UUID.randomUUID().toString();
        List<Map<String, Object>> chunkMetadata = new ArrayList<>();
        
        int totalChunks = (int) Math.ceil((double) data.length / CHUNK_SIZE);
        
        for (int i = 0; i < totalChunks; i++) {
            int start = (int) (i * CHUNK_SIZE);
            int end = (int) Math.min(start + CHUNK_SIZE, data.length);
            byte[] chunk = Arrays.copyOfRange(data, start, end);
            
            String chunkPublicId = uploadChunk(chunk, i, uploadId);
            
            Map<String, Object> chunkInfo = new HashMap<>();
            chunkInfo.put("index", i);
            chunkInfo.put("publicId", chunkPublicId);
            chunkInfo.put("size", chunk.length);
            chunkMetadata.add(chunkInfo);
        }
        
        MediaUploadResponse response = new MediaUploadResponse();
        response.setType("VIDEO");
        response.setCloudinaryPublicId(uploadId);
        response.setUrl(getChunkUrl(chunkMetadata.get(0).get("publicId").toString()));
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("chunked", true);
        metadata.put("totalChunks", totalChunks);
        metadata.put("totalSize", data.length);
        metadata.put("chunks", chunkMetadata);
        response.setMetadata(metadata);
        
        return response;
    }
    
    private String uploadChunk(byte[] chunk, int chunkIndex, String uploadId) throws IOException {
        Map<String, Object> uploadParams = ObjectUtils.asMap(
            "resource_type", "video",
            "folder", "chat/videos/chunks/" + uploadId,
            "public_id", String.format("chunk_%03d", chunkIndex),
            "use_filename", false,
            "unique_filename", false
        );
        
        @SuppressWarnings("unchecked")
        Map<String, Object> uploadResult = cloudinary.uploader().upload(chunk, uploadParams);
        return uploadResult.get("public_id").toString();
    }
    
    public String getMediaUrl(String publicId) {
        return cloudinary.url()
            .secure(true)
            .generate(publicId);
    }
    
    private String getChunkUrl(String publicId) {
        return cloudinary.url()
            .resourceType("video")
            .secure(true)
            .generate(publicId);
    }
    
    public void deleteMedia(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.error("Failed to delete media: {}", e.getMessage());
            throw new AppException(ErrorCode.MEDIA_NOT_FOUND);
        }
    }
    
    public byte[] downloadMedia(String publicId) {
        try {
            String url = getMediaUrl(publicId);
            return url.getBytes();
        } catch (Exception e) {
            log.error("Failed to download media: {}", e.getMessage());
            throw new AppException(ErrorCode.MEDIA_NOT_FOUND);
        }
    }
    
    private MediaUploadResponse buildMediaUploadResponse(Map<String, Object> uploadResult, String type, long fileSize) {
        MediaUploadResponse response = new MediaUploadResponse();
        response.setType(type);
        response.setCloudinaryPublicId(uploadResult.get("public_id").toString());
        response.setUrl(uploadResult.get("secure_url").toString());
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("format", uploadResult.get("format"));
        metadata.put("size", fileSize);
        
        if (uploadResult.containsKey("width")) {
            metadata.put("width", uploadResult.get("width"));
        }
        if (uploadResult.containsKey("height")) {
            metadata.put("height", uploadResult.get("height"));
        }
        if (uploadResult.containsKey("duration")) {
            metadata.put("duration", uploadResult.get("duration"));
        }
        
        response.setMetadata(metadata);
        return response;
    }
}
