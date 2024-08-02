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
    void logUserActivity(Long userId, String username, String deviceName, String details);

}
