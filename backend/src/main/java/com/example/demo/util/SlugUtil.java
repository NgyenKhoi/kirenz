package com.example.demo.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtil {
    
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern EDGES_DASHES = Pattern.compile("(^-|-$)");
    
    public static String slugify(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = EDGES_DASHES.matcher(slug).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
    
    public static String generateShortHash(String id) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(id.getBytes(StandardCharsets.UTF_8));
            
            // Convert to base62-like string (alphanumeric)
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < 3; i++) {
                int value = hash[i] & 0xFF;
                result.append(Integer.toString(value, 36));
            }
            
            return result.toString().substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            // Fallback to simple hash
            return Integer.toHexString(id.hashCode()).substring(0, 6);
        }
    }
    
    public static String generateHybridSlug(String title, String id) {
        String titleSlug = slugify(title);
        String hash = generateShortHash(id);
        
        // Limit title slug to 50 characters for reasonable URL length
        if (titleSlug.length() > 50) {
            titleSlug = titleSlug.substring(0, 50);
        }
        
        return titleSlug + "-" + hash;
    }
    
    public static String extractHashFromSlug(String slug) {
        if (slug == null || slug.isEmpty()) {
            return null;
        }
        
        int lastDash = slug.lastIndexOf('-');
        if (lastDash == -1 || lastDash == slug.length() - 1) {
            return null;
        }
        
        return slug.substring(lastDash + 1);
    }
}
