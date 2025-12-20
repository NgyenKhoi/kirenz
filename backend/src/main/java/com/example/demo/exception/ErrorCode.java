package com.example.demo.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNEXPECTED_EXCEPTION(9999, "undefined exception", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_NOT_FOUND(1001, "User not found", HttpStatus.NOT_FOUND),
    PROFILE_NOT_FOUND(1002, "Profile not found", HttpStatus.NOT_FOUND),
    POST_NOT_FOUND(2001, "Post not found", HttpStatus.NOT_FOUND),
    COMMENT_NOT_FOUND(3001, "Comment not found", HttpStatus.NOT_FOUND),
    INVALID_REQUEST(4001, "Invalid request data", HttpStatus.BAD_REQUEST),
    USER_ALREADY_EXISTS(1003, "User with this email already exists", HttpStatus.CONFLICT),
    INVALID_CREDENTIALS(1004, "Invalid email or password", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(1005, "Token has expired", HttpStatus.UNAUTHORIZED),
    INVALID_TOKEN(1006, "Invalid token", HttpStatus.UNAUTHORIZED),
    PASSWORD_TOO_SHORT(1007, "Password must be at least 8 characters", HttpStatus.BAD_REQUEST),
    PREMIUM_REQUIRED(1008, "Premium subscription required to access this resource", HttpStatus.FORBIDDEN),
    
    CONVERSATION_NOT_FOUND(5001, "Conversation not found", HttpStatus.NOT_FOUND),
    MESSAGE_NOT_FOUND(5002, "Message not found", HttpStatus.NOT_FOUND),
    INVALID_CONVERSATION_TYPE(5003, "Invalid conversation type", HttpStatus.BAD_REQUEST),
    USER_NOT_IN_CONVERSATION(5004, "User is not a participant in this conversation", HttpStatus.FORBIDDEN),
    EMPTY_MESSAGE_CONTENT(5005, "Message content cannot be empty", HttpStatus.BAD_REQUEST),
    INVALID_PARTICIPANT_LIST(5006, "Participant list must contain at least 2 users", HttpStatus.BAD_REQUEST),
    
    MEDIA_UPLOAD_FAILED(6001, "Failed to upload media to Cloudinary", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_MEDIA_TYPE(6002, "Invalid media type. Supported types: IMAGE, VIDEO", HttpStatus.BAD_REQUEST),
    MEDIA_TOO_LARGE(6003, "Media file exceeds maximum size limit", HttpStatus.BAD_REQUEST),
    MEDIA_NOT_FOUND(6004, "Media not found in Cloudinary", HttpStatus.NOT_FOUND),
    INVALID_MEDIA_FORMAT(6005, "Invalid media format", HttpStatus.BAD_REQUEST),
    
    RABBITMQ_PUBLISH_FAILED(7001, "Failed to publish message to RabbitMQ", HttpStatus.INTERNAL_SERVER_ERROR),
    RABBITMQ_CONSUME_FAILED(7002, "Failed to consume message from RabbitMQ", HttpStatus.INTERNAL_SERVER_ERROR),
    MESSAGE_QUEUE_FULL(7003, "Message queue is full, please try again later", HttpStatus.SERVICE_UNAVAILABLE),
    
    WEBSOCKET_CONNECTION_FAILED(8001, "Failed to establish WebSocket connection", HttpStatus.INTERNAL_SERVER_ERROR),
    WEBSOCKET_SESSION_NOT_FOUND(8002, "WebSocket session not found", HttpStatus.NOT_FOUND),
    INVALID_WEBSOCKET_MESSAGE(8003, "Invalid WebSocket message format", HttpStatus.BAD_REQUEST),
    RATE_LIMIT_EXCEEDED(8004, "Rate limit exceeded. Please slow down.", HttpStatus.TOO_MANY_REQUESTS),
    
    INVALID_MESSAGE_CONTENT(9001, "Message content contains invalid or unsafe characters", HttpStatus.BAD_REQUEST);
    
    private int code;
    private String message;
    private HttpStatusCode statusCode;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }
}
