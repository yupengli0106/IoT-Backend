package com.demo.myapp.service.impl;

import com.demo.myapp.pojo.UserActivity;
import com.demo.myapp.service.UserActivityService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
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

    private final Logger logger = LoggerFactory.getLogger(UserActivityServiceImpl.class);

    private final int MAX_ACTIVITIES = 100; // 每个用户最多保存100条活动记录
    private final int FIXED_RATE = 1000 * 60 * 60 * 24 * 7; // 每7天清理一次


    /**
     * !定时任务，清理用户活动记录。
     * !需要在配置类（或 MyAppApplication.class）上加上@EnableScheduling注解
     */
    @Scheduled(fixedRate = FIXED_RATE) // 每7天清理一次
    public void cleanupUserActivities() {
        try{
            // 获取所有用户的ID
            List<Long> userIds = userActivityMapper.findAllUserIds();
            // 删除每个用户的多余活动记录
            //TODO: 这里目前写死只能保存最近的100条记录，后期可以改进为根据用户设置的参数来保存
            for (Long userId : userIds) {
                userActivityMapper.deleteExcessActivities(userId, MAX_ACTIVITIES);
            }
        }catch (Exception e){
            logger.error("Failed to cleanup user activities: {}", e.getMessage());
        }
    }

    @Override
    public List<UserActivity> getUserActivities() {
        Long userId = userService.getCurrentUserId();
        return  userActivityMapper.findUserActivities(userId);
    }

    // 插入一条记录
    @Override
    @Async("taskExecutor")
    public void logUserActivity(Long userId, String username, String deviceName, String details) {
        UserActivity userActivity = new UserActivity();
        userActivity.setUserId(userId);
        userActivity.setUsername(username);
        userActivity.setDeviceName(deviceName);
        userActivity.setDetails(details);
        try {
            userActivityMapper.insertUserActivity(userActivity);
        } catch (Exception e) {
            logger.error("Failed to log user activity: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // 批量插入
    @Async("taskExecutor")
    public void logUserActivities(List<UserActivity> activities) {
        try {
            userActivityMapper.insertActivities(activities);
        } catch (Exception e) {
            logger.error("Failed to log user activities: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
