package com.demo.myapp.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @Author: Yupeng Li
 * @Date: 2/7/2024 00:32
 * @Description: JWT utility class for generating and parsing JWT tokens
 * 
 * SECURITY IMPROVEMENTS:
 * - Externalized secret key from configuration
 * - Added secret key validation
 * - Improved token blacklist management with TTL
 * - Added correlation ID for better logging traceability
 */

@Component
public class JwtUtil {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // IMPROVED: Externalized JWT configuration from application.yml
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.issuer}")
    private String issuer;
    
    @Value("${jwt.expiration}")
    private long expirationTime;

    private Algorithm algorithm;
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);
    
    // IMPROVED: Cache key prefix for better Redis key management
    private static final String BLACKLIST_KEY_PREFIX = "jwt:blacklist:";

    /**
     * IMPROVED: Initialize and validate JWT configuration after dependency injection
     * This ensures the secret key meets security requirements before any JWT operations
     */
    @PostConstruct
    private void initializeJwtConfiguration() {
        // Validate secret key strength
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret key cannot be null or empty. Please set jwt.secret in application.yml or JWT_SECRET environment variable.");
        }
        
        if (secretKey.length() < 32) {
            throw new IllegalStateException("JWT secret key must be at least 32 characters long for security. Current length: " + secretKey.length());
        }
        
        // Initialize algorithm with validated secret
        this.algorithm = Algorithm.HMAC256(secretKey);
        
        logger.info("JWT configuration initialized successfully with issuer: {} and expiration: {}ms", 
                   issuer, expirationTime);
    }

    /**
     * Generates a JWT token
     * @param claims Business data such as user id, username, etc.
     * @return token
     */
    public String generateToken(Map<String, Object> claims) {
        // IMPROVED: Add correlation ID for request tracing
        String correlationId = MDC.get("correlationId");
        
        try {
            String token = JWT.create()
                    .withClaim("userClaims", claims) // 更明确地命名claims
                    .withIssuer(issuer) // IMPROVED: Use configurable issuer
                    .withIssuedAt(new Date()) // 添加令牌发行时间
                    .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))// IMPROVED: Use configurable expiration
                    .withClaim("correlationId", correlationId) // IMPROVED: Add tracing support
                    .sign(algorithm); // 签名
                    
            logger.debug("JWT token generated successfully for user: {}", claims.get("username"));
            return token;
        } catch (Exception e) {
            logger.error("Error generating JWT token for user: {} - {}", claims.get("username"), e.getMessage());
            throw new RuntimeException("Failed to generate JWT token", e);
        }
    }

    /**
     * Parses a JWT token
     * @param token The token to parse
     * @return User claims as a Claim object
     */
    public Map<String, Object> parseToken(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer) // IMPROVED: Use configurable issuer
                    .build(); // 创建JWT验证器

            DecodedJWT jwt = verifier.verify(token); // 使用验证器验证并解码JWT
            
            // IMPROVED: Restore correlation ID for request tracing
            String correlationId = jwt.getClaim("correlationId").asString();
            if (correlationId != null) {
                MDC.put("correlationId", correlationId);
            }
            
            return jwt.getClaim("userClaims").asMap(); // 直接返回 Map
        } catch (JWTVerificationException e) {
            // JWT验证失败，可能是因为签名不匹配、过期等原因
            logger.error("Error parsing JWT token: {}", e.getMessage());
            throw new RuntimeException("Invalid or expired JWT token", e);
        }
    }

    /**
     * IMPROVED: Validates a JWT token with better error handling and blacklist management
     * @param token The token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean isValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        // IMPROVED: Check if the token is on blacklist with proper key prefix
        String blacklistKey = BLACKLIST_KEY_PREFIX + token;
        Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            logger.debug("Token found in blacklist, rejecting access");
            return false;
        }
        
        // Verify the token signature and expiration
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer) // IMPROVED: Use configurable issuer
                    .build(); // 创建JWT验证器

            verifier.verify(token); // 验证JWT
            return true;
        } catch (JWTVerificationException e) {
            logger.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * IMPROVED: Add token to blacklist with proper TTL to prevent memory leaks
     * @param token The token to blacklist
     */
    public void blacklistToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        
        try {
            // Get token expiration to set appropriate TTL
            Date expirationDate = getTokenExpirationTime(token);
            long ttlSeconds = (expirationDate.getTime() - System.currentTimeMillis()) / 1000;
            
            if (ttlSeconds > 0) {
                String blacklistKey = BLACKLIST_KEY_PREFIX + token;
                redisTemplate.opsForValue().set(blacklistKey, "blacklisted", ttlSeconds, TimeUnit.SECONDS);
                logger.debug("Token blacklisted with TTL: {} seconds", ttlSeconds);
            }
        } catch (Exception e) {
            logger.error("Failed to blacklist token: {}", e.getMessage());
        }
    }

    /**
     * Get the expiration time of a JWT token
     * @param token The token to check
     * @return The expiration time of the token
     */
    public Date getTokenExpirationTime(String token) {
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(issuer) // IMPROVED: Use configurable issuer
                    .build();

            DecodedJWT jwt = verifier.verify(token);
            return jwt.getExpiresAt(); // 直接返回过期时间
        } catch (JWTVerificationException e) {
            logger.error("Error getting token expiration time: {}", e.getMessage());
            throw new RuntimeException("Unable to get token expiration time", e);
        }
    }

    /**
     * IMPROVED: Enhanced token extraction with multiple sources support
     * Extracts the token from cookies or Authorization header
     * @param request The HTTP request
     * @return The token if found, null otherwise
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        // First try to get token from cookies (existing behavior)
        String token = extractTokenFromCookies(request);
        if (token != null) {
            return token;
        }
        
        // IMPROVED: Also support Authorization header for API clients
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7); // Remove "Bearer " prefix
        }
        
        return null; // Return null if the token is not found in any source
    }
    
    /**
     * Extracts the token from the cookies in the request
     * @param request The HTTP request
     * @return The token if found, null otherwise
     */
    public String extractTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("httpOnlyToken".equals(cookie.getName())) {
                    return cookie.getValue(); // Return the token from the cookie
                }
            }
        }
        return null; // Return null if the token is not found
    }
}
