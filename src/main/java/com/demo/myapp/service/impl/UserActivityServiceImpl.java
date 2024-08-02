package com.demo.myapp.service.impl;

import com.demo.myapp.pojo.UserActivity;
import com.demo.myapp.service.UserActivityService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import com.demo.myapp.mapper.UserActivityMapper;


import java.util.List;

/**
 * @Author: Yupeng Li
 * @Date: 2/8/2024 23:43
 * @Description:
 */
@Service
public class UserActivityServiceImpl implements UserActivityService {
    @Resource
    private UserActivityMapper userActivityMapper;
    @Resource
    private UserService userService;

    @Override
    public List<UserActivity> getUserActivities() {
        Long userId = userService.getCurrentUserId();
        return  userActivityMapper.findUserActivities(userId);
    }

    @Override
    public void logUserActivity(Long userId, String username, String deviceName, String details) {
        UserActivity userActivity = new UserActivity();
        userActivity.setUserId(userId);
        userActivity.setUsername(username);
        userActivity.setDeviceName(deviceName);
        userActivity.setDetails(details);
        //TODO: 后期改进限制数据库只能保存最近的100条记录
        userActivityMapper.insertUserActivity(userActivity);
    }
}
