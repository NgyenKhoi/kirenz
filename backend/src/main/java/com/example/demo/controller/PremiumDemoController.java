package com.example.demo.controller;

import com.example.demo.annotation.RequiresPremium;
import com.example.demo.dto.response.ApiResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Demo controller to showcase premium authorization functionality
 * Contains endpoints that require premium access
 */
@RestController
@RequestMapping("/api/premium")
public class PremiumDemoController {

    /**
     * Demo endpoint that requires premium access
     * Returns premium content only for users with premium subscription
     * 
     * @param authentication Spring Security Authentication object containing JWT
     * @return Premium content with user information
     */
    @GetMapping("/content")
    @RequiresPremium
    public ApiResponse<Map<String, Object>> getPremiumContent(Authentication authentication) {
        Map<String, Object> premiumData = new HashMap<>();
        
        // Extract user information from JWT
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String userId = jwt.getSubject();
            String email = jwt.getClaim("email");
            Boolean isPremium = jwt.getClaim("premium");
            
            premiumData.put("message", "Welcome to premium content!");
            premiumData.put("userId", userId);
            premiumData.put("email", email);
            premiumData.put("isPremium", isPremium);
            premiumData.put("premiumFeatures", new String[]{
                "Advanced Analytics",
                "Priority Support",
                "Exclusive Content",
                "Ad-Free Experience"
            });
        }
        
        return ApiResponse.success(premiumData, "Premium content retrieved successfully");
    }

    /**
     * Public endpoint for comparison - accessible to all authenticated users
     * 
     * @param authentication Spring Security Authentication object containing JWT
     * @return Basic content available to all users
     */
    @GetMapping("/info")
    public ApiResponse<Map<String, Object>> getPublicInfo(Authentication authentication) {
        Map<String, Object> publicData = new HashMap<>();
        
        // Extract user information from JWT
        if (authentication.getPrincipal() instanceof Jwt jwt) {
            String userId = jwt.getSubject();
            String email = jwt.getClaim("email");
            Boolean isPremium = jwt.getClaim("premium");
            
            publicData.put("message", "This is public content available to all authenticated users");
            publicData.put("userId", userId);
            publicData.put("email", email);
            publicData.put("isPremium", isPremium);
            publicData.put("upgradeMessage", isPremium ? 
                "You already have premium access!" : 
                "Upgrade to premium for exclusive features!");
        }
        
        return ApiResponse.success(publicData, "Public info retrieved successfully");
    }
}
