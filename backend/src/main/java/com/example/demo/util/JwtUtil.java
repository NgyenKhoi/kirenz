package com.example.demo.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Utility class for extracting JWT claims from SecurityContext
 */
public class JwtUtil {

    /**
     * Get current authenticated user ID from JWT
     * @return User ID or null if not authenticated
     */
    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String subject = jwt.getSubject();
            return subject != null ? Long.parseLong(subject) : null;
        }
        return null;
    }

    /**
     * Get current authenticated user email from JWT
     * @return User email or null if not authenticated
     */
    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaim("email");
        }
        return null;
    }

    /**
     * Get current authenticated user premium status from JWT
     * @return Premium status or false if not authenticated
     */
    public static Boolean getCurrentUserPremiumStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            Boolean premium = jwt.getClaim("premium");
            return premium != null ? premium : false;
        }
        return false;
    }

    /**
     * Get the current JWT token
     * @return Jwt object or null if not authenticated
     */
    public static Jwt getCurrentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt;
        }
        return null;
    }
}
