package com.demo.myapp.utils;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @Author: Yupeng Li
 * @Date: 28/7/2025 19:54
 * @Description:
 */
@Component
public class RateLimitUtil {
    @Resource
    RedisTemplate<String, Object> redisTemplate;

    private static final Logger log = LoggerFactory.getLogger(RateLimitUtil.class);

    // IMPROVED: Constants for better maintainability
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 5;

    /**
     * Get client IP address from request
     */
    public String getClientIpAddress(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            // In the case of multiple IPs, the first one is the original client
            return ipAddress.split(",")[0].trim();
        }

        ipAddress = request.getHeader("Proxy-Client-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        ipAddress = request.getHeader("WL-Proxy-Client-IP");
        if (ipAddress != null && !ipAddress.isEmpty() && !"unknown".equalsIgnoreCase(ipAddress)) {
            return ipAddress;
        }

        // Fallback to the remote address
        return request.getRemoteAddr();
    }

    /**
     * Check if a client is rate-limited for a specific action
     */
    public boolean isRateLimited(String clientIp, String action) {
        String correlationId = MDC.get("correlationId");

        String key = RATE_LIMIT_PREFIX + action + ":" + clientIp;
        Object attemptsObj = redisTemplate.opsForValue().get(key);

        if (attemptsObj == null) return false;// No attempts recorded, not rate-limited

        int attempts;
        if (attemptsObj instanceof Number) {
            attempts = ((Number) attemptsObj).intValue();
        } else {
            log.error("Invalid rate limit attempts value for key {}: {} [{}]", key, attemptsObj, correlationId);
            return false;
        }

        return attempts >= MAX_LOGIN_ATTEMPTS;
    }

    /**
     * Increment rate limit counter for failed attempts
     */
    public void incrementRateLimit(String clientIp, String action) {
        String key = RATE_LIMIT_PREFIX + action + ":" + clientIp;
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, java.time.Duration.ofMinutes(RATE_LIMIT_WINDOW_MINUTES));
    }

    /**
     * Clear rate limit counter on successful authentication
     */
    public void clearRateLimit(String clientIp, String action) {
        String key = RATE_LIMIT_PREFIX + action + ":" + clientIp;
        redisTemplate.delete(key);
    }
}
