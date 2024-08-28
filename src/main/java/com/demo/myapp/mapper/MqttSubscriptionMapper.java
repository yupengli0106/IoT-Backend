package com.demo.myapp.mapper;

import com.demo.myapp.pojo.MqttSubscription;
import org.apache.ibatis.annotations.*;

import java.util.List;

/**
 * @Author: Yupeng Li
 * @Date: 28/8/2024 17:24
 * @Description:
 */

@Mapper
public interface MqttSubscriptionMapper {
    @Insert("INSERT INTO mqtt_subscriptions (topic, user_id) VALUES (#{topic}, #{userId})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(MqttSubscription mqttSubscription);

    @Delete("DELETE FROM mqtt_subscriptions WHERE topic = #{topic} AND user_id = #{userId}")
    void delete(String topic, long userId);

    @Select("SELECT topic FROM mqtt_subscriptions")
    List<String> findAllTopics();

}
