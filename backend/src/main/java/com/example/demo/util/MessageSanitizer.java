package com.example.demo.util;

import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
@Slf4j
public class MessageSanitizer {
    
    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
        "<script[^>]*>.*?</script>", 
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
        "<[^>]+>", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN = Pattern.compile(
        "javascript:", 
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ON_EVENT_PATTERN = Pattern.compile(
        "\\bon\\w+\\s*=", 
        Pattern.CASE_INSENSITIVE
    );
    
    public String sanitize(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        String sanitized = content;
        
        // Check for script tags
        if (SCRIPT_PATTERN.matcher(sanitized).find()) {
            log.warn("Detected script tag in message content");
            throw new AppException(ErrorCode.INVALID_MESSAGE_CONTENT);
        }
        
        // Check for javascript: protocol
        if (JAVASCRIPT_PROTOCOL_PATTERN.matcher(sanitized).find()) {
            log.warn("Detected javascript: protocol in message content");
            throw new AppException(ErrorCode.INVALID_MESSAGE_CONTENT);
        }
        
        // Check for event handlers (onclick, onload, etc.)
        if (ON_EVENT_PATTERN.matcher(sanitized).find()) {
            log.warn("Detected event handler in message content");
            throw new AppException(ErrorCode.INVALID_MESSAGE_CONTENT);
        }
        
        // Remove HTML tags
        sanitized = HTML_TAG_PATTERN.matcher(sanitized).replaceAll("");
        
        // Escape special HTML characters
        sanitized = escapeHtml(sanitized);
        
        log.debug("Sanitized message content");
        return sanitized;
    }
    
    private String escapeHtml(String text) {
        if (text == null) {
            return null;
        }
        
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;");
    }
    
    public void validateLength(String content, int maxLength) {
        if (content != null && content.length() > maxLength) {
            log.warn("Message content exceeds maximum length: {} > {}", content.length(), maxLength);
            throw new AppException(ErrorCode.INVALID_MESSAGE_CONTENT);
        }
    }
}
