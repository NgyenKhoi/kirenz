package com.example.demo.filter;

import com.example.demo.annotation.RequiresPremium;
import com.example.demo.exception.AppException;
import com.example.demo.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;

/**
 * Filter to check premium status for endpoints annotated with @RequiresPremium
 * Returns 403 Forbidden if user is not premium
 */
@Component
public class PremiumAuthorizationFilter extends OncePerRequestFilter {

    private final RequestMappingHandlerMapping handlerMapping;

    public PremiumAuthorizationFilter(
            @Qualifier("requestMappingHandlerMapping") RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Get the handler for this request
            HandlerExecutionChain handlerChain = handlerMapping.getHandler(request);
            
            if (handlerChain != null && handlerChain.getHandler() instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handlerChain.getHandler();
                
                // Check if method or class has @RequiresPremium annotation
                boolean requiresPremium = handlerMethod.hasMethodAnnotation(RequiresPremium.class) ||
                                        handlerMethod.getBeanType().isAnnotationPresent(RequiresPremium.class);
                
                if (requiresPremium) {
                    // Get authentication from SecurityContext (set by OAuth2 Resource Server)
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    
                    if (authentication == null || !authentication.isAuthenticated()) {
                        // No authentication - throw exception
                        AppException exception = new AppException();
                        exception.setErrorCode(ErrorCode.PREMIUM_REQUIRED);
                        throw exception;
                    }
                    
                    // Check premium status from JWT claims
                    boolean hasPremiumAccess = hasPremiumAccess(authentication);
                    
                    if (!hasPremiumAccess) {
                        // User is not premium - throw exception
                        AppException exception = new AppException();
                        exception.setErrorCode(ErrorCode.PREMIUM_REQUIRED);
                        throw exception;
                    }
                }
            }
        } catch (Exception e) {
            // Log error but continue - let other filters handle authentication issues
            logger.debug("Error checking premium status: " + e.getMessage());
        }

        // Continue filter chain
        filterChain.doFilter(request, response);
    }

    /**
     * Check if user has premium access from JWT claims
     * 
     * @param authentication Spring Security Authentication object
     * @return true if user has premium access, false otherwise
     */
    private boolean hasPremiumAccess(Authentication authentication) {
        try {
            if (authentication.getPrincipal() instanceof Jwt jwt) {
                Boolean isPremium = jwt.getClaim("premium");
                return isPremium != null && isPremium;
            }
            return false;
        } catch (Exception e) {
            logger.debug("Error extracting premium status: " + e.getMessage());
            return false;
        }
    }
}
