package com.demo.myapp.service.impl;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * @Author: Yupeng Li
 * @Date: 23/9/2024 21:32
 * @Description: IMPROVED - Cache management service with production-safe Redis operations
 * 
 * CRITICAL FIXES:
 * - Replaced dangerous KEYS operation with cache key tracking
 * - Added proper cache key management to avoid memory leaks
 * - Implemented industry-standard cache invalidation patterns
 */
@Service
public class CachedDeviceService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(CachedDeviceService.class);
    
    // IMPROVED: Cache key prefixes for better organization
    private static final String DEVICE_STATS_PREFIX = "deviceStats_";
    private static final String DEVICES_BY_PAGE_PREFIX = "devicesByPage_";
    private static final String CACHE_KEYS_SUFFIX = ":cache_keys";

    @CacheEvict(value = "deviceStats", key = "'deviceStats_' + #userId")
    public void clearDeviceStatsCache(long userId) {
        logger.info("Cleared deviceStats cache for user {}", userId);
    }

    /**
     * PRODUCTION-SAFE REPLACEMENT for dangerous KEYS operation
     * 
     * Instead of using a KEYS pattern (which blocks Redis), we:
     * 1. Track cache keys in a separate Set for each user
     * 2. Delete tracked keys directly without scanning
     * 3. Clean up the tracking set afterward
     * 
     * @param currentUserId 当前用户ID
     */
    public void clearDevicesByPageCache(long currentUserId) {
        String trackingSetKey = DEVICES_BY_PAGE_PREFIX + currentUserId + CACHE_KEYS_SUFFIX;
        
        try {
            // Get all tracked cache keys for this user
            Set<Object> trackedKeys = redisTemplate.opsForSet().members(trackingSetKey);
            
            if (trackedKeys != null && !trackedKeys.isEmpty()) {
                // Convert to String array for batch deletion
                String[] keysToDelete = trackedKeys.stream()
                    .map(Object::toString)
                    .toArray(String[]::new);
                
                // Batch delete all tracked cache keys
                redisTemplate.delete(Set.of(keysToDelete));
                
                // Clean up the tracking set
                redisTemplate.delete(trackingSetKey);
                
                logger.info("Cleared {} devicesByPage cache entries for user {}", 
                          trackedKeys.size(), currentUserId);
            } else {
                logger.debug("No devicesByPage cache entries found for user {}", currentUserId);
            }
        } catch (Exception e) {
            logger.error("Error clearing devicesByPage cache for user {}: {}", currentUserId, e.getMessage());
            
            // FALLBACK: If tracking failed, try the old method as last resort (with warning)
            logger.warn("Falling back to KEYS operation (not recommended for production)");
            fallbackClearDevicesByPageCache(currentUserId);
        }
    }
    
    /**
     * IMPROVED: Method to register a cache key for tracking
     * This should be called whenever a devicesByPage cache entry is created
     */
    public void trackDevicesByPageCacheKey(long userId, String cacheKey) {
        String trackingSetKey = DEVICES_BY_PAGE_PREFIX + userId + CACHE_KEYS_SUFFIX;
        
        try {
            redisTemplate.opsForSet().add(trackingSetKey, cacheKey);
            // Set TTL on a tracking set to prevent memory leaks (slightly longer than cache TTL)
            redisTemplate.expire(trackingSetKey, java.time.Duration.ofMinutes(15));
            
            logger.debug("Registered cache key for tracking: {}", cacheKey);
        } catch (Exception e) {
            logger.error("Failed to track cache key {}: {}", cacheKey, e.getMessage());
        }
    }
    
    /**
     * DANGEROUS FALLBACK METHOD - Only use when tracking system fails
     * This method uses KEYS operation which should NEVER be used in production Redis
     */
    private void fallbackClearDevicesByPageCache(long currentUserId) {
        String pattern = "*devicesByPage_" + currentUserId + "_*";
        
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                logger.warn("FALLBACK: Cleared {} devicesByPage cache entries using KEYS operation", keys.size());
            }
        } catch (Exception e) {
            logger.error("Fallback cache clearing also failed: {}", e.getMessage());
        }
    }
}
