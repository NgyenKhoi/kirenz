package com.example.demo.service;

import com.example.demo.entities.User;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    public String generateAccessToken(User user) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .claim("email", user.getEmail())
                    .claim("premium", user.getIsPremium())
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            JWSSigner signer = new MACSigner(secret.getBytes());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error generating access token", e);
        }
    }

    public String generateRefreshToken(User user) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(user.getId().toString())
                    .claim("email", user.getEmail())
                    .claim("premium", user.getIsPremium())
                    .claim("type", "refresh")
                    .issueTime(now)
                    .expirationTime(expiryDate)
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader(JWSAlgorithm.HS256),
                    claimsSet
            );

            JWSSigner signer = new MACSigner(secret.getBytes());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error generating refresh token", e);
        }
    }

    public JWTClaimsSet validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(secret.getBytes());
            
            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Invalid token signature");
            }

            JWTClaimsSet claimsSet = signedJWT.getJWTClaimsSet();
            
            if (isTokenExpired(claimsSet)) {
                throw new RuntimeException("Token has expired");
            }

            return claimsSet;
        } catch (ParseException e) {
            throw new RuntimeException("Error parsing token", e);
        } catch (JOSEException e) {
            throw new RuntimeException("Error verifying token", e);
        }
    }

    private boolean isTokenExpired(JWTClaimsSet claimsSet) {
        Date expirationTime = claimsSet.getExpirationTime();
        return expirationTime != null && expirationTime.before(new Date());
    }

    public Long extractUserId(String token) {
        try {
            JWTClaimsSet claimsSet = validateToken(token);
            String subject = claimsSet.getSubject();
            return Long.parseLong(subject);
        } catch (Exception e) {
            throw new RuntimeException("Error extracting user ID from token", e);
        }
    }

    public Boolean extractPremiumStatus(String token) {
        try {
            JWTClaimsSet claimsSet = validateToken(token);
            Object premiumClaim = claimsSet.getClaim("premium");
            return premiumClaim != null ? (Boolean) premiumClaim : false;
        } catch (Exception e) {
            throw new RuntimeException("Error extracting premium status from token", e);
        }
    }

    public String extractEmail(String token) {
        try {
            JWTClaimsSet claimsSet = validateToken(token);
            return (String) claimsSet.getClaim("email");
        } catch (Exception e) {
            throw new RuntimeException("Error extracting email from token", e);
        }
    }

    public boolean validateRefreshToken(String token) {
        try {
            JWTClaimsSet claimsSet = validateToken(token);
            String tokenType = (String) claimsSet.getClaim("type");
            return "refresh".equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    public Long extractUserIdFromRefreshToken(String token) {
        try {
            JWTClaimsSet claimsSet = validateToken(token);
            String tokenType = (String) claimsSet.getClaim("type");
            if (!"refresh".equals(tokenType)) {
                throw new RuntimeException("Not a refresh token");
            }
            String subject = claimsSet.getSubject();
            return Long.parseLong(subject);
        } catch (Exception e) {
            throw new RuntimeException("Error extracting user ID from refresh token", e);
        }
    }

    public void invalidateRefreshToken(String token) {
        // In a real implementation, you would store invalidated tokens in a blacklist
        // For now, this is a placeholder
    }
}
