package com.demo.myapp.mapper;

import com.demo.myapp.pojo.UserActivity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Author: Yupeng Li
 * @Date: 2/8/2024 23:46
 * @Description:
 */
@Mapper
public interface UserActivityMapper {

    @Select("SELECT * FROM user_activities WHERE user_id = #{userId}")
    List<UserActivity> findUserActivities(Long userId);

    // 插入一条记录
    @Insert("INSERT INTO user_activities (user_id, username, device_name, details) VALUES (#{userId}, #{username}, #{deviceName}, #{details})")
    void insertUserActivity(UserActivity userActivity);

    // 批量插入
    void insertActivities(@Param("activities") List<UserActivity> activities);

    @Select("SELECT DISTINCT user_id FROM user_activities")
    List<Long> findAllUserIds();

    void deleteExcessActivities(@Param("userId")  Long userId, @Param("maxActivities") int maxActivities);
}
