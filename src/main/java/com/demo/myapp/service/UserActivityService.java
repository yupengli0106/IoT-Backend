package com.demo.myapp.service;

import com.demo.myapp.pojo.UserActivity;

import java.util.List;

/**
 * @Author: Yupeng Li
 * @Date: 2/8/2024 23:42
 * @Description:
 */
public interface UserActivityService {
    List<UserActivity> getUserActivities();
    // 插入一条记录
    void logUserActivity(Long userId, String username, String deviceName, String details);
    // 批量插入
    void logUserActivities(List<UserActivity> userActivities);

}
