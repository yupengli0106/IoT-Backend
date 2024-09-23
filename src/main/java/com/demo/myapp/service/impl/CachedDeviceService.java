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
 * @Description:
 */
@Service
public class CachedDeviceService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(CachedDeviceService.class);

    @CacheEvict(value = "deviceStats", key = "'deviceStats_' + #userId")
    public void clearDeviceStatsCache(long userId) {
        logger.info("Cleared deviceStats cache for user {}", userId);
    }

    /**
     * 由于@CacheEvict注解不支持通配符，所以用redisTemplate通过模式匹配的方式清除缓存
     * @param currentUserId 当前用户ID
     */
    public void clearDevicesByPageCache(long currentUserId) {
        String pattern = "*devicesByPage_" + currentUserId + "_*";  // 构建用户相关的缓存键的模式
        Set<String> keys = redisTemplate.keys(pattern);

        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);  // 批量删除匹配的键
            logger.info("Cleared devicesByPage cache for user {}", currentUserId);
        }
    }
}
