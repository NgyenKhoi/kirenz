package com.example.demo.util;

import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtils {
    
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
        
        if ("anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
        
        try {
            return Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }
}
