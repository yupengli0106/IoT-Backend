package com.demo.myapp.mapper;

import com.demo.myapp.pojo.UserActivity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

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

    @Insert("INSERT INTO user_activities (user_id, username, device_name, details) VALUES (#{userId}, #{username}, #{deviceName}, #{details})")
    void insertUserActivity(UserActivity userActivity);
}
