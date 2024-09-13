package com.demo.myapp.service.impl;

import com.demo.myapp.handler.WebSocketHandler;
import com.demo.myapp.mapper.DeviceMapper;
import com.demo.myapp.mapper.EnergyMapper;
import com.demo.myapp.mapper.MqttSubscriptionMapper;
import com.demo.myapp.pojo.Energy;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.List;


/**
 * @Author: Yupeng Li
 * @Date: 12/7/2024 15:05
 * @Description:
 */

@Service
public class MqttService {
    @Resource
    private IMqttClient mqttClient;
    @Resource
    private WebSocketHandler webSocketHandler;
    @Resource
    MqttSubscriptionMapper mqttSubscriptionMapper;
    @Resource
    private EnergyMapper energyMapper;
    @Resource
    private DeviceMapper deviceMapper;

    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);


    /**
     * 这里是为了在服务端重启的时候，通过查询数据库，重新关注所有之前已经存在关注的topic
     */
    @PostConstruct
    public void resubscribeAllTopics() {
        try {
            mqttSubscriptionMapper.findAllTopics()
                    .forEach(topic -> {
                        try {
                            this.subscribe(topic);
                        } catch (MqttException e) {
                            logger.error("Failed to subscribe to topic: {}", topic);
                        }
                    });
        } catch (Exception e) {
            logger.error("Failed to find topics throw mqttSubscriptionMapper", e);
        }
        logger.info("Resubscribed to all topics in database successfully!");
    }

    /**
     * 发布消息
     * @param topic 主题
     * @param payload 消息内容
     * @throws MqttException 异常
     */
    public void publish(String topic, String payload) throws MqttException {
        MqttMessage message = new MqttMessage(payload.getBytes());
        /*
         * QoS 级别简述:
         * 	1.	QoS 0 - 最多一次 (At most once)
         * 	•	解释：消息最多传递一次，不保证消息到达。
         * 	2.	QoS 1 - 至少一次 (At least once)
         * 	•	解释：消息至少传递一次，确保消息到达，但可能重复。
         * 	3.	QoS 2 - 只有一次 (Exactly once)
         * 	•	解释：消息仅传递一次，确保不重复也不丢失
         */
        message.setQos(1);
        mqttClient.publish(topic, message);
        logger.info("Published message to topic {}: {}", topic, payload);
    }

    /**
     * 订阅主题
     * @param topic 主题
     * @throws MqttException 异常
     */
    public void subscribe(String topic) throws MqttException {
        mqttClient.subscribe(topic, (t, msg) -> {
            String payload = new String(msg.getPayload());
            logger.info("Received message on topic {}: {}", t, payload);
            System.out.println("Received message: " + payload);
            // 处理接收到的消息
            handleIncomingData(payload);
        });
        logger.info("Subscribed to topic: {}", topic);
    }

    /**
     * 批量取消订阅
     * @param topics 主题列表
     * @throws MqttException 异常
     */
    public void unsubscribe(List<String> topics) throws MqttException {
        mqttClient.unsubscribe(topics.toArray(new String[0]));
        logger.info("Unsubscribed from topic: {}", topics);
    }

    /**
     * 处理接收到的设备数据，根据数据类型进行不同的处理。energy数据存储到数据库，其他数据通过WebSocket发送到前端展示。
     * @param data 数据内容
     */
    private void handleIncomingData(String data) {
        //如果是energy数据，直接存储到数据库
        JSONObject jsonObject = new JSONObject(data);//将字符串转换为json对象

        String sensorType = jsonObject.getString("sensorType");
        if ("energy".equals(sensorType)) {
            // ! 存储能耗数据, 这里要注意jsonObject.getLong还是jsonObject.getString等
            long deviceId = jsonObject.getLong("deviceId");
            Date recordDate = Date.valueOf(jsonObject.getString("date"));
            BigDecimal totalEnergy = BigDecimal.valueOf(jsonObject.getDouble("totalEnergy"));

            /*
                * 通过设备ID找到用户ID, 不能通过securityContext获取当前用户ID，因为这里是异步处理消息，不在请求线程中。
                * 通过securityContext获取当前用户ID的方法只能在请求线程中使用，通常是与HTTP请求相关的操作才能获取到。
                * 这里是MQTT消息处理，不在请求线程中，所以要通过其他方式获取当前用户ID。
                * 还因为这里是存入数据库，因为也不能依赖物理设备的用户ID，设备不一定是存用户ID, 降低耦合性。
               TODO: 这里是否可以改进？
             */
            long userId = deviceMapper.findDeviceById(deviceId).getUserId();
            if (userId<=0) {//如果userId<=0，说明没有找到对应的设备
                logger.error("Failed to get user id by device id: {}", deviceId);
                throw new RuntimeException("Failed to get user id by device id: " + deviceId);
            }

            Energy energy = new Energy();
            energy.setDeviceId(deviceId);
            energy.setEnergy(totalEnergy);
            energy.setRecordDate(recordDate);
            energy.setUserId(userId);

            try {
                // 这里是接受模拟器的数据，且energy数据是每日结算出当天的总能耗才发送过来。
                energyMapper.insertEnergy(energy);
            } catch (Exception e) {
                logger.error("Failed to store energy data: {}", data);
            }

            logger.info("Stored energy data successfully: {}", data);
        }

        // 如果是温度，湿度等数据，通过 WebSocket 发送数据到客户端, 用于实时更新UI
        try {
            webSocketHandler.sendMessageToClients(data);
            logger.info("WebSocket successfully sent message to clients: {}", data);
        } catch (Exception e) {
            logger.error("Failed to send message to clients: {}", data);
        }

    }
}
