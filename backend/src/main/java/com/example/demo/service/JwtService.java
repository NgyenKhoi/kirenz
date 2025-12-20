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

    /**
     * Generate access token for authenticated user
     * @param user The authenticated user
     * @return JWT access token string
     */
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

    /**
     * Generate refresh token for authenticated user
     * @param user The authenticated user
     * @return JWT refresh token string
     */
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

    /**
     * Validate JWT token signature and expiration
     * @param token JWT token string
     * @return JWTClaimsSet if token is valid
     * @throws RuntimeException if token is invalid or expired
     */
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

    /**
     * Check if token is expired
     * @param claimsSet JWT claims set
     * @return true if token is expired, false otherwise
     */
    private boolean isTokenExpired(JWTClaimsSet claimsSet) {
        Date expirationTime = claimsSet.getExpirationTime();
        return expirationTime != null && expirationTime.before(new Date());
    }

    /**
     * Extract user ID from JWT token
     * @param token JWT token string
     * @return User ID
     */
    public Long extractUserId(String token) {
        try {
            JWTClaimsSet claimsSet = validateToken(token);
            String subject = claimsSet.getSubject();
            return Long.parseLong(subject);
        } catch (Exception e) {
            throw new RuntimeException("Error extracting user ID from token", e);
        }
    }

    /**
     * Extract premium status from JWT token
     * @param token JWT token string
     * @return Premium status
     */
    public Boolean extractPremiumStatus(String token) {
        try {
            JWTClaimsSet claimsSet = validateToken(token);
            Object premiumClaim = claimsSet.getClaim("premium");
            return premiumClaim != null ? (Boolean) premiumClaim : false;
        } catch (Exception e) {
            throw new RuntimeException("Error extracting premium status from token", e);
        }
    }

    /**
     * Extract email from JWT token
     * @param token JWT token string
     * @return User email
     */
    public String extractEmail(String token) {
        try {
            JWTClaimsSet claimsSet = validateToken(token);
            return (String) claimsSet.getClaim("email");
        } catch (Exception e) {
            throw new RuntimeException("Error extracting email from token", e);
        }
    }
}
