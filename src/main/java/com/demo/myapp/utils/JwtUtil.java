package com.demo.myapp.utils;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.annotation.Resource;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

/**
 * @Author: Yupeng Li
 * @Date: 2/7/2024 00:32
 * @Description: JWT utility class for generating and parsing JWT tokens
 */

@Component
public class JwtUtil {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // TODO: when in production, store following values in environment variables or configuration files instead of hardcoding them
    private static final String SECRET_KEY = "*** I bet you can't guess this secret key hhh ***";
    private static final String ISSUER = "MyApp"; // application name
    private static final long EXPIRATION_TIME = 24 * 60 * 60 * 1000; // 24h

    private static final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY); // 初始化算法
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    /**
     * Generates a JWT token
     * @param claims Business data such as user id, username, etc.
     * @return token
     */
    public String generateToken(Map<String, Object> claims) {
        try {
            return JWT.create()
                    .withClaim("userClaims", claims) // 更明确地命名claims
                    .withIssuer(ISSUER) // 添加发行者
                    .withIssuedAt(new Date()) // 添加令牌发行时间
                    .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME))// 添加过期时间
                    .sign(algorithm); // 签名
        } catch (Exception e) {
            logger.error("Error generating token", e);
            throw new RuntimeException("Error generating token", e);
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
                    .withIssuer(ISSUER)
                    .build(); // 创建JWT验证器

            DecodedJWT jwt = verifier.verify(token); // 使用验证器验证并解码JWT
            return jwt.getClaim("userClaims").asMap(); // 直接返回 Map
        } catch (JWTVerificationException e) {
            // JWT验证失败，可能是因为签名不匹配、过期等原因
            logger.error("Error parsing token", e);
            throw new RuntimeException("Error parsing token", e);
        }
    }

    /**
     * Validates a JWT token
     * @param token The token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean isValidToken(String token) {
        //check if the token on blacklist
        if (Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token))) {
            return false;
        }
        // verify the token
        try {
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build(); // 创建JWT验证器

            verifier.verify(token); // 验证JWT
            return true;
        } catch (JWTVerificationException e) {
            return false;
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
                    .withIssuer(ISSUER)
                    .build();

            DecodedJWT jwt = verifier.verify(token);
            return jwt.getExpiresAt(); // 直接返回过期时间
        } catch (JWTVerificationException e) {
            logger.error("Error getting token expiration time", e);
            throw new RuntimeException("Error getting token expiration time", e);
        }
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
                if (cookie.getName().equals("httpOnlyToken")) {
                    return cookie.getValue(); // Return the token from the cookie
                }
            }
        }
        return null; // Return null if the token is not found
    }
}
