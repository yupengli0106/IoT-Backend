package com.demo.myapp.service.impl;

import com.demo.myapp.mapper.PermissionMapper;
import com.demo.myapp.mapper.RoleMapper;
import com.demo.myapp.mapper.UserMapper;
import com.demo.myapp.pojo.User;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @Author: Yupeng Li
 * @Date: 22/9/2024 02:58
 * @Description: This class is used to cache user, role and permission data in Redis cache.
 * when a new API request comes in, it will go through the JWT filter,
 * and then the CachedUserService will be called to get the user, role and permission data from the cache.
 * <p>
 * If the data is not in the cache, it will be fetched from the database and then stored in the cache.
 * If the data is already in the cache, it will be fetched from the cache directly.
 * <p>
 * when the user, role and permission data is updated, the cache will be evicted.
 * <p>
 * The aim of this class is to reduce the number of database queries and improve the performance of the application.
 */
@Service
public class CachedUserService {
    @Resource
    private UserMapper userMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private PermissionMapper permissionMapper;

    private static final Logger logger = LoggerFactory.getLogger(CachedUserService.class);

    @Cacheable(value = "users", key = "#userId")
    public User getUserById(Long userId) {
        return userMapper.finUserById(userId);
    }

    @Cacheable(value = "roles", key = "#userId")
    public List<String> getRolesByUserId(Long userId) {
        return roleMapper.findRolesByUserId(userId);
    }

    @Cacheable(value = "permissions", key = "#userId")
    public List<String> getPermissionsByUserId(Long userId) {
        return permissionMapper.findPermissionsByUserId(userId);
    }

    @CacheEvict(value = "users", key = "#userId")
    public void evictUserCache(Long userId) {
        logger.info("User cache evicted: {}", userId);
    }

    @CacheEvict(value = "roles", key = "#userId")
    public void evictRolesCache(Long userId) {
        logger.info("Roles cache evicted: {}", userId);
    }

    @CacheEvict(value = "permissions", key = "#userId")
    public void evictPermissionsCache(Long userId) {
        logger.info("Permissions cache evicted: {}", userId);
    }
}
